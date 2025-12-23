package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.LoggerAdapter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

/**
 * Abstract base client for Ripple SDK
 */
abstract class RippleClient(
    protected val config: RippleConfig
) {
    internal val metadataManager = MetadataManager()
    protected val dispatcher = Dispatcher(
        config = Dispatcher.DispatcherConfig(
            endpoint = config.endpoint,
            apiKey = config.apiKey,
            apiKeyHeader = config.apiKeyHeader,
            flushInterval = config.flushInterval,
            maxBatchSize = config.maxBatchSize,
            maxRetries = config.maxRetries
        ),
        httpAdapter = config.adapters.httpAdapter,
        storageAdapter = config.adapters.storageAdapter,
        loggerAdapter = config.adapters.loggerAdapter
    )
    
    protected val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    protected var isInitialized = false

    /**
     * Initialize the client and restore any persisted events from storage.
     * Must be called before tracking events.
     */
    open fun init() {
        executor.execute {
            dispatcher.restore()
            dispatcher.startScheduledFlush(executor)
            isInitialized = true
            config.adapters.loggerAdapter?.info("RippleClient initialized")
        }
    }

    /**
     * Track an event with optional payload and metadata.
     * 
     * @param name Event name identifier
     * @param payload Optional event data as key-value pairs
     * @param metadata Optional event-specific metadata that merges with shared metadata
     */
    fun track(
        name: String,
        payload: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null
    ) {
        if (!isInitialized) {
            throw IllegalStateException("Client must be initialized before tracking events")
        }

        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = mergeMetadata(metadata),
            sessionId = getSessionId(),
            platform = getPlatform()
        )

        executor.execute {
            dispatcher.enqueue(event)
        }
    }

    /**
     * Set shared metadata that will be attached to all subsequent events.
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    fun setMetadata(key: String, value: Any) {
        metadataManager.set(key, value)
    }

    /**
     * Immediately flush all queued events to the server.
     */
    fun flush() {
        executor.execute {
            dispatcher.flush()
        }
    }

    /**
     * Clean up resources and stop background operations.
     * Call when the client is no longer needed.
     */
    open fun dispose() {
        executor.execute {
            dispatcher.dispose()
        }
        executor.shutdown()
        config.adapters.loggerAdapter?.info("RippleClient disposed")
    }

    /**
     * Get session ID for the current client instance.
     * Platform-specific implementation.
     */
    protected abstract fun getSessionId(): String?

    /**
     * Get platform information for event context.
     * Platform-specific implementation.
     */
    protected abstract fun getPlatform(): Platform?

    private fun mergeMetadata(eventMetadata: Map<String, Any>?): Map<String, Any>? {
        val globalMetadata = metadataManager.getAll()
        
        return when {
            globalMetadata.isEmpty() && eventMetadata.isNullOrEmpty() -> null
            globalMetadata.isEmpty() -> eventMetadata
            eventMetadata.isNullOrEmpty() -> globalMetadata
            else -> globalMetadata + eventMetadata
        }
    }
}
