package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.coroutines.*
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
    private val queue = Queue<Event>()
    private val flushMutex = Mutex()
    private var flushJob: Job? = null
    private var isDisposed = false

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
    suspend fun enqueue(event: Event) {
        if (isDisposed) return
        
        queue.enqueue(event)
        loggerAdapter?.debug("Event enqueued: ${event.name}")
        
        if (queue.size() >= config.maxBatchSize) {
            flush()
        }
    }

    /**
     * Send queued events
     */
    suspend fun flush() {
        if (isDisposed) return
        
        flushMutex.runAtomic {
            val events = mutableListOf<Event>()
            
            // Dequeue all events
            while (!queue.isEmpty()) {
                queue.dequeue()?.let { events.add(it) }
            }
            
            if (events.isEmpty()) return@runAtomic
            
            loggerAdapter?.info("Flushing ${events.size} events")
            
            var attempt = 0
            var success = false
            
            while (attempt < config.maxRetries && !success && !isDisposed) {
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
                        delay(delay)
                    }
                }
            }
            
            if (!success) {
                // Re-queue events and persist
                events.forEach { queue.enqueue(it) }
                storageAdapter.save(queue.toList())
                loggerAdapter?.error("Failed to send events after ${config.maxRetries} attempts")
            }
        }
    }

    /**
     * Load persisted events
     */
    suspend fun restore() {
        try {
            val events = storageAdapter.load()
            queue.fromList(events)
            loggerAdapter?.info("Restored ${events.size} persisted events")
        } catch (e: Exception) {
            loggerAdapter?.error("Failed to restore events: ${e.message}")
        }
    }

    /**
     * Start scheduled flushing
     */
    fun startScheduledFlush(scope: CoroutineScope) {
        flushJob = scope.launch {
            while (!isDisposed) {
                delay(config.flushInterval)
                if (!isDisposed && !queue.isEmpty()) {
                    flush()
                }
            }
        }
    }

    /**
     * Clean up resources
     */
    fun dispose() {
        isDisposed = true
        flushJob?.cancel()
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = 1000L
        val exponentialDelay = baseDelay * (2.0.pow(attempt - 1)).toLong()
        val jitter = Random.nextLong(0, 1000)
        return exponentialDelay + jitter
    }
}
