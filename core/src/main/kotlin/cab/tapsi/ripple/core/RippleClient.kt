package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.ConsoleLoggerAdapter

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
 *         override fun getPayload() = buildJsonObject {
 *             put("email", email)
 *         }
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
    private val loggerAdapter = config.adapters.loggerAdapter ?: ConsoleLoggerAdapter()
    private var dispatcher: Dispatcher? = null
    private var anonymousId: String = ""
    private var userId: String? = null
    val events: EventsNamespace = EventsNamespace(this)
    
    @Volatile
    protected var isInitialized = false

    @Volatile
    private var isDisposed = false

    /**
     * Initialize the client.
     * Called automatically by track() when needed. Can be called after dispose().
     */
    open fun init() {
        if (isInitialized) return
        
        synchronized(this) {
            if (isInitialized) return

            if (isDisposed) {
                isDisposed = false
            }

            anonymousId = loadAnonymousId() ?: anonymousId.takeIf { it.isNotBlank() } ?: generateAnonymousId()
            saveAnonymousId(anonymousId)
            userId = userId ?: loadUserId()

            initializeStorageAdapter()
            dispatcher = createDispatcher()
            dispatcher?.restore()

            isInitialized = true
            loggerAdapter.info("RippleClient initialized")
        }
    }

    // ==================== TYPE-SAFE TRACK METHODS ====================

    /**
     * Track a type-safe event.
     *
     * @param event Event implementing [RippleEvent]
     */
    fun track(event: TEvents) {
        trackInternal(event.name, event.getPayload(), event.schemaVersion)
    }

    internal fun trackPredefined(
        name: String,
        payload: kotlinx.serialization.json.JsonObject,
        schemaVersion: String? = PREDEFINED_SCHEMA_VERSION
    ) {
        trackInternal(name, payload, schemaVersion)
    }

    // ==================== UNTYPED TRACK METHODS ====================

    /**
     * Track an untyped event.
     *
     * @param name Event name
     * @param payload Optional payload
     * @param schemaVersion Optional event schema version
     */
    @JvmOverloads
    fun track(
        name: String,
        payload: Map<String, Any>? = null,
        schemaVersion: String? = null
    ) {
        trackInternal(name, payload?.toJsonObject(), schemaVersion)
    }

    protected fun trackInternal(
        name: String,
        payload: kotlinx.serialization.json.JsonObject?,
        schemaVersion: String?
    ) {
        if (isDisposed) {
            loggerAdapter.warn("Cannot track event: Client has been disposed")
            return
        }

        init()

        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = metadataManager.getAll(),
            platform = getPlatform(),
            sdk = getSdkInfo(),
            anonymousId = anonymousId,
            eventId = generateEventId(),
            schemaVersion = schemaVersion,
            userId = userId
        )

        dispatcher?.enqueue(event)
    }

    // ==================== V2 IDENTITY & PREDEFINED METHODS ====================

    fun identify(userId: String, traits: UserTraits = UserTraits()) {
        this.userId = userId
        saveUserId(userId)
        trackPredefined("user_identified", UserIdentifiedPayload(userId, traits).toJsonPayload())
    }

    fun clicked(payload: ClickedPayload) {
        trackPredefined("clicked", payload.toJsonPayload())
    }

    fun viewed(payload: ViewedPayload) {
        trackPredefined("viewed", payload.toJsonPayload())
    }

    fun screen(payload: ScreenPayload) {
        trackPredefined("screened", payload.toJsonPayload())
    }

    fun appOpened() {
        trackPredefined("app_state_changed", AppStateChangedPayload(AppState.OPENED).toJsonPayload())
    }

    fun appClosed() {
        trackPredefined("app_state_changed", AppStateChangedPayload(AppState.CLOSED).toJsonPayload())
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
    fun getMetadata(): Map<String, Any>? = metadataManager.getAll().ifEmpty { null }


    /**
     * Clear all metadata.
     */
    fun clearMetadata() {
        metadataManager.clear()
    }

    // ==================== IDENTITY & FLUSH ====================

    fun getAnonymousId(): String = anonymousId

    fun getUserId(): String? = userId

    /**
     * Flush queued events. Non-blocking.
     */
    fun flush() {
        if (!isInitialized) return
        dispatcher?.flush(force = true)
    }

    /**
     * Get the number of queued events.
     */
    fun getQueueSize(): Int = dispatcher?.getQueueSize() ?: 0

    /**
     * Dispose the client. Supports re-initialization via init().
     */
    open fun dispose() {
        synchronized(this) {
            dispatcher?.dispose()
            dispatcher = null
            metadataManager.clear()
            isDisposed = true
            isInitialized = false
            loggerAdapter.info("RippleClient disposed")
        }
    }

    // ==================== ABSTRACT ====================

    protected abstract fun getPlatform(): Platform?

    protected open fun generateAnonymousId(): String = IdGenerator.generate()

    protected open fun generateEventId(): String = IdGenerator.generate()

    protected open fun getSdkInfo(): SdkInfo = DEFAULT_SDK_INFO

    protected open fun loadAnonymousId(): String? = null

    protected open fun saveAnonymousId(anonymousId: String) = Unit

    protected open fun loadUserId(): String? = null

    protected open fun saveUserId(userId: String?) = Unit

    private fun initializeStorageAdapter() {
        try {
            config.adapters.storageAdapter.init()
        } catch (e: Exception) {
            loggerAdapter.error(
                "Failed to initialize storage adapter",
                mapOf("error" to (e.message ?: e::class.java.simpleName))
            )
        }
    }

    private fun createDispatcher(): Dispatcher {
        return Dispatcher(
            config = Dispatcher.DispatcherConfig(
                endpoint = config.endpoint,
                apiKey = config.apiKey,
                apiKeyHeader = config.apiKeyHeader,
                flushInterval = config.resolvedFlushInterval,
                maxBatchSize = config.resolvedMaxBatchSize,
                maxRetries = config.resolvedMaxRetries,
                maxPayloadSize = config.batchOptions.maxPayloadSize ?: 64L * 1024L,
                maxBufferSize = config.maxBufferSize,
                eventTtl = config.eventTtl,
                retryOptions = config.retryOptions,
                eventSampler = config.eventSampler,
                hooks = createTelemetryHooks(config.hooks)
            ),
            httpAdapter = config.adapters.httpAdapter,
            storageAdapter = config.adapters.storageAdapter,
            loggerAdapter = loggerAdapter
        )
    }

    private fun createTelemetryHooks(userHooks: TelemetryHooks): TelemetryHooks {
        val options = config.telemetryOptions
        if (options == null || options.disabled || options.endpoint.isBlank()) return userHooks

        val reporter = TelemetryReporter(
            endpoint = options.endpoint,
            apiKey = config.apiKey,
            apiKeyHeader = config.apiKeyHeader,
            httpAdapter = config.adapters.httpAdapter,
            loggerAdapter = loggerAdapter,
            getAnonymousId = { anonymousId },
            getUserId = { userId },
            getMetadata = { getMetadata() },
            getPlatform = { getPlatform() },
            getSdk = { getSdkInfo() },
            generateEventId = { generateEventId() }
        )

        return TelemetryHooks(
            onFlush = {
                reporter.reportFlush(it)
                userHooks.onFlush?.invoke(it)
            },
            onSendSuccess = {
                reporter.reportSendSuccess(it)
                userHooks.onSendSuccess?.invoke(it)
            },
            onSendFailure = {
                reporter.reportSendFailure(it)
                userHooks.onSendFailure?.invoke(it)
            },
            onRetry = {
                reporter.reportRetry(it)
                userHooks.onRetry?.invoke(it)
            },
            onDrop = {
                reporter.reportDrop(it)
                userHooks.onDrop?.invoke(it)
            },
            onEnqueue = {
                reporter.reportEnqueue(it)
                userHooks.onEnqueue?.invoke(it)
            }
        )
    }

    companion object {
        private val DEFAULT_SDK_INFO = SdkInfo(
            name = "ripple-kotlin",
            version = RippleClient::class.java.`package`?.implementationVersion ?: "unknown"
        )

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
