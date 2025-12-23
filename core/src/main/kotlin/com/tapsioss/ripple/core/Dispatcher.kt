package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.pow
import kotlin.random.Random

/**
 * Queue management, batching, and retry logic
 */
class Dispatcher(
    private val config: DispatcherConfig,
    private val httpAdapter: HttpAdapter,
    private val storageAdapter: StorageAdapter,
    private val loggerAdapter: LoggerAdapter?
) {
    private val eventQueue = ConcurrentLinkedQueue<Event>()
    private val isDisposed = AtomicBoolean(false)
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
     * Add event to queue
     */
    fun enqueue(event: Event) {
        if (isDisposed.get()) return
        
        eventQueue.offer(event)
        loggerAdapter?.debug("Event enqueued: ${event.name}")
        
        if (eventQueue.size >= config.maxBatchSize) {
            flush()
        }
    }

    /**
     * Send queued events
     */
    fun flush() {
        if (isDisposed.get()) return
        
        synchronized(this) {
            val events = mutableListOf<Event>()
            
            // Drain all events from queue
            while (eventQueue.isNotEmpty()) {
                eventQueue.poll()?.let { events.add(it) }
            }
            
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
                        throw Exception("HTTP ${response.status}")
                    }
                } catch (e: Exception) {
                    attempt++
                    loggerAdapter?.warn("Flush attempt $attempt failed: ${e.message}")
                    
                    if (attempt < config.maxRetries) {
                        val delay = calculateBackoffDelay(attempt)
                        loggerAdapter?.debug("Retrying in ${delay}ms")
                        Thread.sleep(delay)
                    }
                }
            }
            
            if (!success) {
                // Re-queue events and persist
                events.forEach { eventQueue.offer(it) }
                storageAdapter.save(events)
                loggerAdapter?.error("Failed to send events after ${config.maxRetries} attempts")
            }
        }
    }

    /**
     * Load persisted events
     */
    fun restore() {
        try {
            val events = storageAdapter.load()
            events.forEach { eventQueue.offer(it) }
            loggerAdapter?.info("Restored ${events.size} persisted events")
        } catch (e: Exception) {
            loggerAdapter?.error("Failed to restore events: ${e.message}")
        }
    }

    /**
     * Start scheduled flushing
     */
    fun startScheduledFlush(executor: ScheduledExecutorService) {
        scheduledFlushTask = executor.scheduleWithFixedDelay({
            if (!isDisposed.get() && eventQueue.isNotEmpty()) {
                flush()
            }
        }, config.flushInterval, config.flushInterval, TimeUnit.MILLISECONDS)
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        isDisposed.set(true)
        scheduledFlushTask?.cancel(false)
    }

    fun getQueueSize(): Int = eventQueue.size

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = 1000L
        val exponentialDelay = baseDelay * (2.0.pow(attempt - 1)).toLong()
        val jitter = Random.nextLong(0, 1000)
        return exponentialDelay + jitter
    }
}
