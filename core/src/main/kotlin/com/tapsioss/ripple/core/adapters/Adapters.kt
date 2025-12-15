package com.tapsioss.ripple.core.adapters

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse

/**
 * HTTP adapter interface for sending events
 */
interface HttpAdapter {
    suspend fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse
}

/**
 * Storage adapter interface for persisting events
 */
interface StorageAdapter {
    suspend fun save(events: List<Event>)
    suspend fun load(): List<Event>
    suspend fun clear()
}

/**
 * Logger adapter interface
 */
interface LoggerAdapter {
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}

/**
 * Log levels
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, NONE
}
