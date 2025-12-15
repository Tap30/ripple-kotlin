package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.pow
import kotlin.random.Random

/**
 * Queue management, batching, and retry logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Dispatcher(
    private val config: DispatcherConfig,
    private val httpAdapter: HttpAdapter,
    private val storageAdapter: StorageAdapter,
    private val loggerAdapter: LoggerAdapter?
) {
    private val eventChannel = Channel<Event>(Channel.UNLIMITED)
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
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun enqueue(event: Event) {
        if (isDisposed) return
        
        eventChannel.trySend(event)
        loggerAdapter?.debug("Event enqueued: ${event.name}")
        
        if (!eventChannel.isEmpty && getQueueSize() >= config.maxBatchSize) {
            flush()
        }
    }

    /**
     * Send queued events
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun flush() {
        if (isDisposed) return
        
        flushMutex.withLock {
            val events = mutableListOf<Event>()
            
            // Drain all events from channel
            while (!eventChannel.isEmpty) {
                eventChannel.tryReceive().getOrNull()?.let { events.add(it) }
            }
            
            if (events.isEmpty()) return@withLock
            
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
                events.forEach { eventChannel.trySend(it) }
                storageAdapter.save(events)
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
            events.forEach { eventChannel.trySend(it) }
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
                if (!isDisposed && !eventChannel.isEmpty) {
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
        eventChannel.close()
    }

    private fun getQueueSize(): Int {
        // Approximate size for batch triggering  
        return if (!eventChannel.isEmpty) config.maxBatchSize else 0
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = 1000L
        val exponentialDelay = baseDelay * (2.0.pow(attempt - 1)).toLong()
        val jitter = Random.nextLong(0, 1000)
        return exponentialDelay + jitter
    }
}
