package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.LoggerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

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
    
    protected val clientScope = CoroutineScope(SupervisorJob())
    protected var isInitialized = false

    /**
     * Initialize the client and restore any persisted events from storage.
     * Must be called before tracking events.
     */
    open suspend fun init() {
        dispatcher.restore()
        dispatcher.startScheduledFlush(clientScope)
        isInitialized = true
        config.adapters.loggerAdapter?.info("RippleClient initialized")
    }

    /**
     * Track an event with optional payload and metadata.
     * 
     * @param name Event name identifier
     * @param payload Optional event data as key-value pairs
     * @param metadata Optional event-specific metadata that merges with shared metadata
     */
    suspend fun track(
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

        dispatcher.enqueue(event)
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
    suspend fun flush() {
        dispatcher.flush()
    }

    /**
     * Clean up resources and stop background operations.
     * Call when the client is no longer needed.
     */
    open fun dispose() {
        dispatcher.dispose()
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
