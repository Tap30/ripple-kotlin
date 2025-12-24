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
 * Thread-safe implementation that processes events asynchronously while
 * providing a simple blocking API. Uses internal threading for HTTP operations
 * to avoid blocking the caller's thread during network requests.
 */
class Dispatcher(
    private val config: DispatcherConfig,
    private val httpAdapter: HttpAdapter,
    private val storageAdapter: StorageAdapter,
    private val loggerAdapter: LoggerAdapter?
) {
    private val eventQueue = ConcurrentLinkedQueue<Event>()
    private val isDisposed = AtomicBoolean(false)
    private val flushLock = ReentrantLock()
    private val isFlushInProgress = AtomicBoolean(false)
    
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(2) { runnable ->
        Thread(runnable, "ripple-dispatcher").apply { isDaemon = true }
    }
    
    private var scheduledFlushTask: java.util.concurrent.ScheduledFuture<*>? = null

    /**
     * Configuration for the dispatcher.
     */
    data class DispatcherConfig(
        val endpoint: String,
        val apiKey: String,
        val apiKeyHeader: String,
        val flushInterval: Long,
        val maxBatchSize: Int,
        val maxRetries: Int
    )

    /**
     * Add event to the queue.
     * Thread-safe and non-blocking.
     * 
     * @param event Event to enqueue
     */
    fun enqueue(event: Event) {
        if (isDisposed.get()) {
            loggerAdapter?.warn("Cannot enqueue event: dispatcher is disposed")
            return
        }
        
        eventQueue.offer(event)
        loggerAdapter?.debug("Event enqueued: ${event.name}, queue size: ${eventQueue.size}")
        
        // Trigger flush if batch size reached
        if (eventQueue.size >= config.maxBatchSize) {
            flush()
        }
    }

    /**
     * Flush all queued events to the server.
     * Non-blocking - submits work to background thread.
     * Safe to call multiple times - concurrent calls are handled gracefully.
     */
    fun flush() {
        if (isDisposed.get()) {
            loggerAdapter?.warn("Cannot flush: dispatcher is disposed")
            return
        }
        
        // Skip if flush already in progress (prevents queue starvation)
        if (!isFlushInProgress.compareAndSet(false, true)) {
            loggerAdapter?.debug("Flush already in progress, skipping")
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
     * Flush events and wait for completion.
     * Blocking call - waits until all events are sent or failed.
     */
    fun flushSync() {
        if (isDisposed.get()) return
        
        flushLock.withLock {
            flushInternal()
        }
    }

    private fun flushInternal() {
        val events = drainQueue()
        if (events.isEmpty()) return
        
        loggerAdapter?.info("Flushing ${events.size} events")
        
        var attempt = 0
        var success = false
        
        while (attempt < config.maxRetries && !success && !isDisposed.get()) {
            try {
                val headers = mapOf(config.apiKeyHeader to config.apiKey)
                val response = httpAdapter.send(config.endpoint, events, headers, config.apiKeyHeader)
                
                if (response.ok) {
                    success = true
                    loggerAdapter?.info("Events sent successfully")
                    storageAdapter.clear()
                } else {
                    throw RuntimeException("HTTP error: ${response.status}")
                }
            } catch (e: Exception) {
                attempt++
                loggerAdapter?.warn("Flush attempt $attempt failed: ${e.message}")
                
                if (attempt < config.maxRetries && !isDisposed.get()) {
                    val delay = calculateBackoffDelay(attempt)
                    loggerAdapter?.debug("Retrying in ${delay}ms")
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
            // Re-queue failed events
            events.forEach { eventQueue.offer(it) }
            storageAdapter.save(events)
            loggerAdapter?.error("Failed to send events after ${config.maxRetries} attempts, re-queued ${events.size} events")
        }
    }

    private fun drainQueue(): List<Event> {
        val events = mutableListOf<Event>()
        while (eventQueue.isNotEmpty()) {
            eventQueue.poll()?.let { events.add(it) }
        }
        return events
    }

    /**
     * Restore persisted events from storage.
     */
    fun restore() {
        if (isDisposed.get()) return
        
        try {
            val events = storageAdapter.load()
            if (events.isNotEmpty()) {
                events.forEach { eventQueue.offer(it) }
                loggerAdapter?.info("Restored ${events.size} persisted events")
            }
        } catch (e: Exception) {
            loggerAdapter?.error("Failed to restore events: ${e.message}")
        }
    }

    /**
     * Start scheduled automatic flushing.
     */
    fun startScheduledFlush() {
        if (isDisposed.get()) return
        
        scheduledFlushTask = executor.scheduleWithFixedDelay(
            {
                if (!isDisposed.get() && eventQueue.isNotEmpty()) {
                    flush()
                }
            },
            config.flushInterval,
            config.flushInterval,
            TimeUnit.MILLISECONDS
        )
        loggerAdapter?.debug("Scheduled flush started with interval ${config.flushInterval}ms")
    }

    /**
     * Get current queue size.
     */
    fun getQueueSize(): Int = eventQueue.size

    /**
     * Clean up resources and stop all background operations.
     */
    fun dispose() {
        if (!isDisposed.compareAndSet(false, true)) {
            return // Already disposed
        }
        
        loggerAdapter?.debug("Disposing dispatcher")
        
        scheduledFlushTask?.cancel(false)
        
        // Attempt final flush
        try {
            val events = drainQueue()
            if (events.isNotEmpty()) {
                storageAdapter.save(events)
                loggerAdapter?.info("Persisted ${events.size} events on dispose")
            }
        } catch (e: Exception) {
            loggerAdapter?.error("Failed to persist events on dispose: ${e.message}")
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
        val jitter = Random.nextLong(0, 500)
        return minOf(exponentialDelay + jitter, 30000L) // Cap at 30 seconds
    }
}
