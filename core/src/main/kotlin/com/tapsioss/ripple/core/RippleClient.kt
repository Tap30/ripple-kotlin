package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.ConsoleLoggerAdapter

/**
 * Abstract base client for Ripple SDK with type-safe generics.
 * 
 * @param TEvents Event type implementing [RippleEvent] for type-safe event tracking
 * @param TMetadata Metadata type implementing [RippleMetadata] for type-safe metadata
 * 
 * ## Type-Safe Usage
 * ```kotlin
 * // Define events
 * sealed class AppEvent : RippleEvent {
 *     data class UserLogin(val email: String) : AppEvent() {
 *         override val name = "user.login"
 *         override fun toPayload() = mapOf("email" to email)
 *     }
 * }
 * 
 * // Define metadata
 * data class AppMetadata(val userId: String) : RippleMetadata {
 *     override fun toMap() = mapOf("userId" to userId)
 * }
 * 
 * // Create typed client
 * val client = RippleClient<AppEvent, AppMetadata>(config)
 * client.track(AppEvent.UserLogin("test@example.com"))
 * client.setMetadata(AppMetadata("user-123"))
 * ```
 * 
 * ## Untyped Usage
 * ```kotlin
 * val client = RippleClient.create(config)
 * client.track("event_name", mapOf("key" to "value"))
 * client.setMetadata("userId", "user-123")
 * ```
 */
abstract class RippleClient<TEvents : RippleEvent, TMetadata : RippleMetadata>(
    protected val config: RippleConfig
) {
    
    private val metadataManager = MetadataManager()
    private var dispatcher: Dispatcher? = null
    private var sessionId: String? = null
    
    @Volatile
    protected var isInitialized = false

    /**
     * Initialize the client.
     * Must be called before tracking events. Can be called after dispose().
     */
    open fun init() {
        if (isInitialized) return
        
        synchronized(this) {
            if (isInitialized) return
            
            sessionId = generateSessionId()
            dispatcher = createDispatcher()
            dispatcher?.restore()
            dispatcher?.startScheduledFlush()
            isInitialized = true
            config.adapters.loggerAdapter?.info("RippleClient initialized")
        }
    }

    // ==================== TYPE-SAFE TRACK METHODS ====================

    /**
     * Track a type-safe event.
     * 
     * @param event Event implementing [RippleEvent]
     */
    fun track(event: TEvents) {
        trackInternal(event.name, event.toPayload(), null)
    }

    /**
     * Track a type-safe event with type-safe metadata.
     * 
     * @param event Event implementing [RippleEvent]
     * @param metadata Type-safe metadata
     */
    fun track(event: TEvents, metadata: TMetadata) {
        trackInternal(event.name, event.toPayload(), metadata.toMap())
    }

    /**
     * Track a type-safe event with untyped metadata.
     * 
     * @param event Event implementing [RippleEvent]
     * @param metadata Optional metadata map
     */
    fun track(event: TEvents, metadata: Map<String, Any>?) {
        trackInternal(event.name, event.toPayload(), metadata)
    }

    // ==================== UNTYPED TRACK METHODS ====================

    /**
     * Track an untyped event.
     * 
     * @param name Event name
     * @param payload Optional payload
     * @param metadata Optional metadata
     */
    @JvmOverloads
    fun track(name: String, payload: Map<String, Any>? = null, metadata: Map<String, Any>? = null) {
        trackInternal(name, payload, metadata)
    }

    /**
     * Track an untyped event with type-safe metadata.
     */
    fun track(name: String, payload: Map<String, Any>?, metadata: TMetadata) {
        trackInternal(name, payload, metadata.toMap())
    }

    private fun trackInternal(name: String, payload: Map<String, Any>?, metadata: Map<String, Any>?) {
        checkInitialized()

        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = metadataManager.merge(metadata),
            sessionId = sessionId,
            platform = getPlatform()
        )

        dispatcher?.enqueue(event)
    }

    // ==================== METADATA METHODS ====================

    /**
     * Set type-safe global metadata.
     * Merges with existing metadata.
     */
    fun setMetadata(metadata: TMetadata) {
        metadata.toMap().forEach { (k, v) -> metadataManager.set(k, v) }
    }

    /**
     * Set a single metadata key-value pair.
     */
    fun setMetadata(key: String, value: Any) {
        metadataManager.set(key, value)
    }

    /**
     * Get all current metadata.
     */
    fun getMetadata(): Map<String, Any> = metadataManager.getAll()

    /**
     * Remove a metadata key.
     */
    fun removeMetadata(key: String) {
        metadataManager.remove(key)
    }

    /**
     * Clear all metadata.
     */
    fun clearMetadata() {
        metadataManager.clear()
    }

    // ==================== SESSION & FLUSH ====================

    /**
     * Get the current session ID.
     */
    fun getSessionId(): String? = sessionId

    /**
     * Flush queued events. Non-blocking.
     */
    fun flush() {
        if (!isInitialized) return
        dispatcher?.flush()
    }

    /**
     * Flush queued events and wait for completion. Blocking.
     */
    fun flushSync() {
        if (!isInitialized) return
        dispatcher?.flushSync()
    }

    /**
     * Get the number of queued events.
     */
    fun getQueueSize(): Int = dispatcher?.getQueueSize() ?: 0

    /**
     * Dispose the client. Supports re-initialization via init().
     */
    open fun dispose() {
        if (!isInitialized) return
        
        synchronized(this) {
            if (!isInitialized) return
            
            dispatcher?.dispose()
            dispatcher = null
            metadataManager.clear()
            sessionId = null
            isInitialized = false
            config.adapters.loggerAdapter?.info("RippleClient disposed")
        }
    }

    // ==================== ABSTRACT ====================

    protected abstract fun getPlatform(): Platform?

    protected open fun generateSessionId(): String = SessionIdGenerator.generate()

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
            throw IllegalStateException("Client not initialized. Call init() before tracking events.")
        }
    }

    companion object {
        /**
         * Create an untyped client for simple usage.
         * Uses default implementations for events and metadata.
         */
        fun create(config: RippleConfig): RippleClient<DefaultRippleEvent, DefaultRippleMetadata> {
            return object : RippleClient<DefaultRippleEvent, DefaultRippleMetadata>(config) {
                override fun getPlatform(): Platform? = null
            }
        }
    }
}
