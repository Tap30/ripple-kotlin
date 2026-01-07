package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.pow
import kotlin.random.Random

/**
 * Event dispatcher handling queue management, batching, and retry logic.
 * 
 * Thread-safe implementation with:
 * - Atomic flush operations (mutex-protected)
 * - FIFO event ordering
 * - Exponential backoff with jitter for retries
 * - Proper 4xx vs 5xx error handling
 */
class Dispatcher(
    private val config: DispatcherConfig,
    private val httpAdapter: HttpAdapter,
    private val storageAdapter: StorageAdapter,
    private val loggerAdapter: LoggerAdapter
) {
    private val eventQueue = ConcurrentLinkedQueue<Event>()
    private val isDisposed = AtomicBoolean(false)
    private val flushLock = ReentrantLock()
    private val isFlushInProgress = AtomicBoolean(false)
    
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { runnable ->
        Thread(runnable, "ripple-dispatcher").apply { isDaemon = true }
    }
    
    private var scheduledFlushTask: java.util.concurrent.ScheduledFuture<*>? = null

    data class DispatcherConfig(
        val endpoint: String,
        val apiKey: String,
        val apiKeyHeader: String,
        val flushInterval: Long,
        val maxBatchSize: Int,
        val maxRetries: Int
    )

    /**
     * Add event to the queue. O(1) operation.
     */
    fun enqueue(event: Event) {
        if (isDisposed.get()) {
            loggerAdapter.warn("Cannot enqueue event: dispatcher is disposed")
            return
        }
        
        eventQueue.offer(event)
        loggerAdapter.debug("Event enqueued: ${event.name}, queue size: ${eventQueue.size}")
        
        if (eventQueue.size >= config.maxBatchSize) {
            flush()
        }
    }

    /**
     * Flush all queued events. Non-blocking, mutex-protected.
     */
    fun flush() {
        if (isDisposed.get()) return
        
        if (!isFlushInProgress.compareAndSet(false, true)) {
            loggerAdapter.debug("Flush already in progress, skipping")
            return
        }
        
        executor.execute {
            try {
                flushInternal()
            } finally {
                isFlushInProgress.set(false)
            }
        }
    }

    /**
     * Flush events and wait for completion. Blocking call.
     */
    fun flushSync() {
        if (isDisposed.get()) return
        flushLock.withLock { flushInternal() }
    }

    private fun flushInternal() {
        val events = drainQueue()
        if (events.isEmpty()) return
        
        loggerAdapter.info("Flushing ${events.size} events")
        
        var attempt = 0
        var success = false
        var shouldRetry = true
        
        while (attempt < config.maxRetries && !success && shouldRetry && !isDisposed.get()) {
            try {
                val headers = mapOf(config.apiKeyHeader to config.apiKey)
                val response = httpAdapter.send(config.endpoint, events, headers, config.apiKeyHeader)
                
                when {
                    response.ok -> {
                        success = true
                        loggerAdapter.info("Events sent successfully")
                        storageAdapter.clear()
                    }
                    response.status in 400..499 -> {
                        // 4xx Client Error - Don't retry, persist events
                        shouldRetry = false
                        loggerAdapter.warn("Client error ${response.status}, not retrying")
                    }
                    else -> {
                        // 5xx Server Error - Retry
                        throw RuntimeException("Server error: ${response.status}")
                    }
                }
            } catch (e: Exception) {
                attempt++
                loggerAdapter.warn("Flush attempt $attempt failed: ${e.message}")
                
                if (attempt < config.maxRetries && shouldRetry && !isDisposed.get()) {
                    val delay = calculateBackoffDelay(attempt)
                    loggerAdapter.debug("Retrying in ${delay}ms")
                    try {
                        Thread.sleep(delay)
                    } catch (ie: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
        }
        
        if (!success && !isDisposed.get()) {
            // Re-queue failed events at the FRONT to maintain FIFO order
            requeue(events)
            storageAdapter.save(events)
            loggerAdapter.error("Failed to send events, re-queued ${events.size} events")
        }
    }

    private fun drainQueue(): List<Event> {
        val events = mutableListOf<Event>()
        while (eventQueue.isNotEmpty()) {
            eventQueue.poll()?.let { events.add(it) }
        }
        return events
    }

    private fun requeue(failedEvents: List<Event>) {
        // Drain current queue, prepend failed events, re-add all
        val currentEvents = drainQueue()
        failedEvents.forEach { eventQueue.offer(it) }
        currentEvents.forEach { eventQueue.offer(it) }
    }

    fun restore() {
        if (isDisposed.get()) return
        
        try {
            val events = storageAdapter.load()
            if (events.isNotEmpty()) {
                events.forEach { eventQueue.offer(it) }
                loggerAdapter.info("Restored ${events.size} persisted events")
            }
        } catch (e: Exception) {
            loggerAdapter.error("Failed to restore events: ${e.message}")
        }
    }

    fun startScheduledFlush() {
        if (isDisposed.get()) return
        
        scheduledFlushTask = executor.scheduleWithFixedDelay(
            { if (!isDisposed.get() && eventQueue.isNotEmpty()) flush() },
            config.flushInterval,
            config.flushInterval,
            TimeUnit.MILLISECONDS
        )
        loggerAdapter.debug("Scheduled flush started with interval ${config.flushInterval}ms")
    }

    fun getQueueSize(): Int = eventQueue.size

    fun dispose() {
        if (!isDisposed.compareAndSet(false, true)) return
        
        loggerAdapter.debug("Disposing dispatcher")
        scheduledFlushTask?.cancel(false)
        
        try {
            val events = drainQueue()
            if (events.isNotEmpty()) {
                storageAdapter.save(events)
                loggerAdapter.info("Persisted ${events.size} events on dispose")
            }
        } catch (e: Exception) {
            loggerAdapter.error("Failed to persist events on dispose: ${e.message}")
        }
        
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = 1000L
        val exponentialDelay = baseDelay * (2.0.pow(attempt - 1)).toLong()
        val jitter = Random.nextLong(0, 1000) // Contract: 0-1000ms jitter
        return minOf(exponentialDelay + jitter, 30000L)
    }
}
