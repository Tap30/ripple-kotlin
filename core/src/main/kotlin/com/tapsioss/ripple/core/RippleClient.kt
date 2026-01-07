package com.tapsioss.ripple.core

/**
 * Abstract base client for Ripple SDK.
 * 
 * Provides a simple, non-blocking API for event tracking with automatic
 * batching, retry logic, and offline persistence. All public methods are
 * thread-safe and can be called from any thread.
 * 
 * Example usage:
 * ```kotlin
 * val client = AndroidRippleClient(context, config)
 * client.init()
 * client.setMetadata("user_id", "12345")
 * client.track("button_clicked", mapOf("button" to "submit"))
 * client.flush()
 * client.dispose()
 * // Can re-initialize after dispose
 * client.init()
 * ```
 * 
 * @param config Configuration for the client
 */
abstract class RippleClient(
    protected val config: RippleConfig
) {
    private val metadataManager = MetadataManager()
    
    private var dispatcher: Dispatcher? = null
    
    @Volatile
    protected var isInitialized = false

    /**
     * Initialize the client.
     * 
     * Restores any persisted events from storage and starts the automatic
     * flush scheduler. Must be called before tracking events.
     * 
     * This method is idempotent - calling it multiple times has no effect.
     * Thread-safe and non-blocking. Can be called after dispose() to
     * re-initialize the client.
     */
    open fun init() {
        if (isInitialized) return
        
        synchronized(this) {
            if (isInitialized) return
            
            dispatcher = createDispatcher()
            dispatcher?.restore()
            dispatcher?.startScheduledFlush()
            isInitialized = true
            config.adapters.loggerAdapter?.info("RippleClient initialized")
        }
    }

    /**
     * Track an event.
     * 
     * Events are queued and sent in batches according to the configuration.
     * This method is non-blocking and returns immediately.
     * 
     * @param name Event name identifier (required)
     * @param payload Optional event data as key-value pairs
     * @param metadata Optional event-specific metadata that merges with global metadata
     * @throws IllegalStateException if client is not initialized
     */
    fun track(
        name: String,
        payload: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null
    ) {
        checkInitialized()

        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = mergeMetadata(metadata),
            sessionId = getSessionId(),
            platform = getPlatform()
        )

        dispatcher?.enqueue(event)
    }

    /**
     * Set global metadata that will be attached to all subsequent events.
     * 
     * Metadata set here is merged with event-specific metadata, with
     * event-specific values taking precedence.
     * 
     * Thread-safe and can be called at any time.
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    fun setMetadata(key: String, value: Any) {
        metadataManager.set(key, value)
    }

    /**
     * Get all stored metadata as a shallow copy.
     * 
     * @return Copy of all metadata, empty map if none set
     */
    fun getMetadata(): Map<String, Any> = metadataManager.getAll()

    /**
     * Remove a global metadata key.
     * 
     * @param key Metadata key to remove
     */
    fun removeMetadata(key: String) {
        metadataManager.remove(key)
    }

    /**
     * Clear all global metadata.
     */
    fun clearMetadata() {
        metadataManager.clear()
    }

    /**
     * Get the current session ID.
     * 
     * Session ID is auto-generated on client creation and persists
     * for the lifetime of the client instance.
     * 
     * @return Current session ID or null if not available
     */
    abstract fun getSessionId(): String?

    /**
     * Flush queued events to the server.
     * 
     * Non-blocking - submits flush work to background thread and returns
     * immediately. Safe to call multiple times; concurrent calls are
     * handled gracefully.
     * 
     * Use [flushSync] if you need to wait for completion.
     */
    fun flush() {
        if (!isInitialized) return
        dispatcher?.flush()
    }

    /**
     * Flush queued events and wait for completion.
     * 
     * Blocking call - waits until all events are sent or failed.
     * Use this when you need to ensure events are sent before proceeding,
     * such as before app termination.
     */
    fun flushSync() {
        if (!isInitialized) return
        dispatcher?.flushSync()
    }

    /**
     * Get the current number of queued events.
     * 
     * @return Number of events waiting to be sent
     */
    fun getQueueSize(): Int = dispatcher?.getQueueSize() ?: 0

    /**
     * Clean up resources and stop background operations.
     * 
     * Persists any unsent events to storage for later retry.
     * After calling dispose, the client can be re-initialized by calling init().
     * 
     * This method is idempotent - calling it multiple times has no effect.
     */
    open fun dispose() {
        if (!isInitialized) return
        
        synchronized(this) {
            if (!isInitialized) return
            
            dispatcher?.dispose()
            dispatcher = null
            isInitialized = false
            config.adapters.loggerAdapter?.info("RippleClient disposed")
        }
    }

    /**
     * Get platform information for event context.
     * Platform-specific implementation.
     */
    protected abstract fun getPlatform(): Platform?

    private fun createDispatcher(): Dispatcher {
        return Dispatcher(
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
            loggerAdapter = config.adapters.loggerAdapter ?: ConsoleLoggerAdapter()
        )
    }

    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("RippleClient must be initialized before tracking events. Call init() first.")
        }
    }

    private fun mergeMetadata(eventMetadata: Map<String, Any>?): Map<String, Any>? {
        val globalMetadata = metadataManager.getAll()
        
        return when {
            globalMetadata.isEmpty() && eventMetadata.isNullOrEmpty() -> null
            globalMetadata.isEmpty() -> eventMetadata
            eventMetadata.isNullOrEmpty() -> globalMetadata
            else -> globalMetadata + eventMetadata // Event metadata takes precedence
        }
    }
}
