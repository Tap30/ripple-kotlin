package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter

/**
 * Configuration for Ripple client
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
 * Adapter configuration
 */
data class AdapterConfig(
    val httpAdapter: HttpAdapter,
    val storageAdapter: StorageAdapter,
    val loggerAdapter: LoggerAdapter? = null
)

/**
 * Event data structure
 */
data class Event(
    val name: String,
    val payload: Map<String, Any>?,
    val issuedAt: Long,
    val metadata: Map<String, Any>?,
    val sessionId: String?,
    val platform: Platform?
)

/**
 * Platform information
 */
sealed class Platform {
    data class Android(
        val device: DeviceInfo,
        val os: OsInfo
    ) : Platform()
    
    data class Server(
        val runtime: String = "JVM"
    ) : Platform()
}

/**
 * Device information
 */
data class DeviceInfo(
    val name: String,
    val version: String
)

/**
 * OS information
 */
data class OsInfo(
    val name: String,
    val version: String
)

/**
 * HTTP response
 */
data class HttpResponse(
    val ok: Boolean,
    val status: Int,
    val data: Any? = null
)
