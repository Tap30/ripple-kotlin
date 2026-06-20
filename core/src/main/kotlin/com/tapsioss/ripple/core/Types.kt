package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Configuration for Ripple client.
 */
data class RippleConfig @JvmOverloads constructor(
    val apiKey: String,
    val endpoint: String,
    val apiKeyHeader: String = "X-API-Key",
    val flushInterval: Long = 10000L,
    val maxBatchSize: Int = 10,
    val maxRetries: Int = 3,
    val adapters: AdapterConfig,
    val batchOptions: BatchOptions = BatchOptions(),
    val retryOptions: RetryOptions = RetryOptions(),
    val maxBufferSize: Int = 50,
    val eventTtl: Long? = null,
    val eventSampler: ((Event) -> Boolean)? = null,
    val hooks: TelemetryHooks = TelemetryHooks(),
    val telemetryOptions: TelemetryOptions? = null
) {
    val resolvedFlushInterval: Long get() = batchOptions.interval ?: flushInterval
    val resolvedMaxBatchSize: Int get() = batchOptions.size ?: maxBatchSize
    val resolvedMaxPayloadSize: Long get() = batchOptions.maxPayloadSize ?: 65536L
    val resolvedMaxRetries: Int get() = retryOptions.maxAttempts ?: maxRetries

    init {
        require(apiKey.isNotBlank()) { "`apiKey` must be provided in `config`." }
        require(endpoint.isNotBlank()) { "`endpoint` must be provided in `config`." }
        batchOptions.interval?.let {
            require(it > 0) { "`batchOptions.interval` must be a positive number." }
        }
        batchOptions.size?.let {
            require(it > 0) { "`batchOptions.size` must be a positive number." }
        }
        batchOptions.maxPayloadSize?.let {
            require(it > 0) { "`batchOptions.maxPayloadSize` must be a positive number." }
        }
        retryOptions.maxAttempts?.let {
            require(it >= 0) { "`retryOptions.maxAttempts` must be a non-negative number." }
        }
        require(retryOptions.minDelay > 0) { "`retryOptions.minDelay` must be a positive number." }
        require(retryOptions.maxDelay > 0) { "`retryOptions.maxDelay` must be a positive number." }
        require(retryOptions.backoffFactor > 0) { "`retryOptions.backoffFactor` must be a positive number." }
        require(maxBufferSize > 0) { "`maxBufferSize` must be a positive number." }
    }
}

/**
 * V2 batch controls. Legacy [RippleConfig.flushInterval] and [RippleConfig.maxBatchSize]
 * remain supported for source compatibility.
 */
data class BatchOptions @JvmOverloads constructor(
    val interval: Long? = null,
    val size: Int? = null,
    val maxPayloadSize: Long? = null
)

/**
 * V2 retry controls. Legacy [RippleConfig.maxRetries] remains supported for source compatibility.
 */
data class RetryOptions @JvmOverloads constructor(
    val maxAttempts: Int? = null,
    val minDelay: Long = 1000L,
    val maxDelay: Long = 360000L,
    val backoffFactor: Double = 2.0
)

/**
 * Adapter configuration.
 */
data class AdapterConfig @JvmOverloads constructor(
    val httpAdapter: HttpAdapter,
    val storageAdapter: StorageAdapter,
    val loggerAdapter: LoggerAdapter? = null
)

/**
 * Event data structure.
 */
@Serializable
data class Event(
    val name: String,
    val payload: JsonObject?,
    val issuedAt: Long,
    val metadata: Map<String, @Serializable(with = AnySerializer::class) Any>?,
    val platform: Platform?,
    val sdk: SdkInfo = SdkInfo(name = "ripple-kotlin", version = "unknown"),
    val anonymousId: String = IdGenerator.generate(),
    val eventId: String = IdGenerator.generate(),
    val schemaVersion: String? = null,
    val userId: String? = null
)

@Serializable
data class SdkInfo(
    val name: String,
    val version: String
)

/**
 * Platform information - discriminated union.
 * 
 * Per API contract:
 * - WebPlatform (type: "web") - browser, device, os
 * - NativePlatform (type: "native") - device, os  
 * - ServerPlatform (type: "server") - no additional fields
 */
@Serializable
sealed class Platform {
    abstract val type: String
    
    @Serializable
    @SerialName("web")
    data class Web(
        val browser: DeviceInfo,
        val device: DeviceInfo,
        val os: DeviceInfo
    ) : Platform() {
        override val type: String = "web"
    }
    
    @Serializable
    @SerialName("native")
    data class Native(
        val device: DeviceInfo,
        val os: DeviceInfo
    ) : Platform() {
        override val type: String = "native"
    }
    
    @Serializable
    @SerialName("server")
    data object Server : Platform() {
        override val type: String = "server"
    }
}

/**
 * Device/OS/Browser information.
 */
@Serializable
data class DeviceInfo(
    val name: String,
    val version: String
)

/**
 * HTTP response from adapter.
 */
data class HttpResponse @JvmOverloads constructor(
    val ok: Boolean,
    val status: Int,
    val data: Any? = null
)

data class TelemetryHooks @JvmOverloads constructor(
    val onFlush: ((FlushInfo) -> Unit)? = null,
    val onSendSuccess: ((SendSuccessInfo) -> Unit)? = null,
    val onSendFailure: ((SendFailureInfo) -> Unit)? = null,
    val onRetry: ((RetryInfo) -> Unit)? = null,
    val onDrop: ((DropInfo) -> Unit)? = null,
    val onEnqueue: ((EnqueueInfo) -> Unit)? = null
)

data class TelemetryOptions @JvmOverloads constructor(
    val endpoint: String,
    val disabled: Boolean = false
)

data class FlushInfo(val eventCount: Int, val batchCount: Int)
data class SendSuccessInfo(val batchSize: Int, val status: Int)
data class SendFailureInfo(val batchSize: Int, val error: String, val attempt: Int)
data class RetryInfo(val attempt: Int, val delay: Long)
data class DropInfo(val eventCount: Int, val reason: DropReason)
data class EnqueueInfo(val bufferSize: Int)

enum class DropReason {
    EXPIRED,
    SAMPLED,
    CLIENT_ERROR,
    BUFFER_FULL
}
