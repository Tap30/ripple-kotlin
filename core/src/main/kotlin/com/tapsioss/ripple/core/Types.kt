package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for Ripple client.
 */
data class RippleConfig(
    val apiKey: String,
    val endpoint: String,
    val apiKeyHeader: String = "X-API-Key",
    val flushInterval: Long = 5000L,
    val maxBatchSize: Int = 10,
    val maxRetries: Int = 3,
    val adapters: AdapterConfig
)

/**
 * Adapter configuration.
 */
data class AdapterConfig(
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
    val payload: Map<String, @Serializable(with = AnySerializer::class) Any>?,
    val issuedAt: Long,
    val metadata: Map<String, @Serializable(with = AnySerializer::class) Any>?,
    val sessionId: String?,
    val platform: Platform?
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
data class HttpResponse(
    val ok: Boolean,
    val status: Int,
    val data: Any? = null
)


