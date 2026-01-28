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
        if (eventQueue.isEmpty()) return
        
        // Process events in batches
        while (eventQueue.isNotEmpty() && !isDisposed.get()) {
            val batch = drainBatch()
            if (batch.isEmpty()) break
            
            loggerAdapter.info("Flushing batch of ${batch.size} events")
            
            var attempt = 0
            
            while (attempt < config.maxRetries && !isDisposed.get()) {
                try {
                    val headers = mapOf(config.apiKeyHeader to config.apiKey)
                    val response = httpAdapter.send(config.endpoint, batch, headers, config.apiKeyHeader)
                    
                    when {
                        response.status in 200..299 -> {
                            // 2xx: Success, clear storage for this batch
                            loggerAdapter.info("Batch sent successfully")
                            break // Move to next batch
                        }
                        response.status in 400..499 -> {
                            // 4xx: Client error, drop batch
                            loggerAdapter.warn("4xx client error, dropping batch", mapOf(
                                "status" to response.status,
                                "eventsCount" to batch.size
                            ))
                            break // Drop this batch, move to next
                        }
                        response.status >= 500 -> {
                            // 5xx: Server error, retry with backoff
                            if (attempt < config.maxRetries - 1) {
                                loggerAdapter.warn("5xx server error, retrying batch", mapOf(
                                    "status" to response.status,
                                    "attempt" to attempt + 1,
                                    "maxRetries" to config.maxRetries
                                ))
                                val delay = calculateBackoffDelay(attempt + 1)
                                Thread.sleep(delay)
                                attempt++
                            } else {
                                // Max retries reached, re-queue batch and persist
                                loggerAdapter.error("5xx server error, max retries reached for batch", mapOf(
                                    "status" to response.status,
                                    "maxRetries" to config.maxRetries,
                                    "eventsCount" to batch.size
                                ))
                                requeueBatch(batch)
                                storageAdapter.save(getAllQueuedEvents())
                                return
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Network error occurred
                    loggerAdapter.error("Network error occurred for batch", mapOf("error" to e.message))
                    
                    if (attempt < config.maxRetries - 1) {
                        loggerAdapter.warn("Network error, retrying batch", mapOf(
                            "attempt" to attempt + 1,
                            "maxRetries" to config.maxRetries,
                            "error" to e.message
                        ))
                        val delay = calculateBackoffDelay(attempt + 1)
                        try {
                            Thread.sleep(delay)
                        } catch (ie: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return
                        }
                        attempt++
                    } else {
                        // Network error, max retries reached for this batch
                        loggerAdapter.error("Network error, max retries reached for batch", mapOf(
                            "maxRetries" to config.maxRetries,
                            "eventsCount" to batch.size,
                            "error" to e.message
                        ))
                        requeueBatch(batch)
                        storageAdapter.save(getAllQueuedEvents())
                        return
                    }
                }
            }
        }
        
        // Clear storage after all successful batches
        if (eventQueue.isEmpty()) {
            storageAdapter.clear()
        }
    }

    private fun drainBatch(): List<Event> {
        val batch = mutableListOf<Event>()
        var count = 0
        while (count < config.maxBatchSize && eventQueue.isNotEmpty()) {
            eventQueue.poll()?.let { 
                batch.add(it)
                count++
            }
        }
        return batch
    }

    private fun requeueBatch(batch: List<Event>) {
        // Maintain FIFO order: failed batch goes FIRST
        val currentEvents = drainQueue()
        val reorderedQueue = batch + currentEvents
        reorderedQueue.forEach { eventQueue.offer(it) }
    }

    private fun getAllQueuedEvents(): List<Event> {
        return eventQueue.toList()
    }

    private fun drainQueue(): List<Event> {
        val events = mutableListOf<Event>()
        while (eventQueue.isNotEmpty()) {
            eventQueue.poll()?.let { events.add(it) }
        }
        return events
    }

    private fun requeue(failedEvents: List<Event>) {
        // Maintain FIFO order: failed events go FIRST, then current events
        val currentEvents = drainQueue()
        val reorderedQueue = failedEvents + currentEvents
        reorderedQueue.forEach { eventQueue.offer(it) }
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
        // Contract: exponential backoff with integer powers
        val exponentialDelay = baseDelay * (2.0.pow(attempt - 1)).toLong()
        val jitter = Random.nextLong(0, 1001) // Contract: 0-1000ms inclusive jitter
        return minOf(exponentialDelay + jitter, 30000L) // Contract: max 30s delay
    }
}
