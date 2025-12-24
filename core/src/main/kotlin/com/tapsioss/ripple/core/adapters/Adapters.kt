package com.tapsioss.ripple.core.adapters

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse

/**
 * HTTP adapter interface for sending events to remote endpoints.
 * 
 * Implementations should handle network operations and return results
 * synchronously. The SDK handles threading internally, so implementations
 * can perform blocking I/O operations.
 * 
 * Example implementation:
 * ```kotlin
 * class MyHttpAdapter : HttpAdapter {
 *     override fun send(endpoint: String, events: List<Event>, headers: Map<String, String>, apiKeyHeader: String): HttpResponse {
 *         // Perform HTTP POST request
 *         return HttpResponse(ok = true, status = 200, data = null)
 *     }
 * }
 * ```
 */
interface HttpAdapter {
    /**
     * Send events to the specified endpoint.
     * 
     * This method may block during network operations. The SDK calls this
     * method from a background thread, so blocking is acceptable.
     * 
     * @param endpoint Target API endpoint URL
     * @param events List of events to send
     * @param headers HTTP headers to include in request
     * @param apiKeyHeader Name of the API key header
     * @return HTTP response with status and optional data
     */
    fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse
}

/**
 * Storage adapter interface for persisting events locally.
 * 
 * Implementations should provide durable storage for events that fail
 * to send, allowing retry on next app launch.
 * 
 * Example implementation:
 * ```kotlin
 * class MyStorageAdapter : StorageAdapter {
 *     override fun save(events: List<Event>) { /* Save to disk */ }
 *     override fun load(): List<Event> = /* Load from disk */
 *     override fun clear() { /* Delete stored events */ }
 * }
 * ```
 */
interface StorageAdapter {
    /**
     * Save events to local storage for later retry.
     * 
     * @param events List of events to persist
     */
    fun save(events: List<Event>)
    
    /**
     * Load previously saved events from storage.
     * 
     * @return List of persisted events, empty list if none found
     */
    fun load(): List<Event>
    
    /**
     * Clear all persisted events from storage.
     */
    fun clear()
}

/**
 * Logger adapter interface for SDK logging output.
 * 
 * Implementations can route logs to any logging framework (Logcat, SLF4J, etc.)
 * 
 * Example implementation:
 * ```kotlin
 * class MyLoggerAdapter : LoggerAdapter {
 *     override fun debug(message: String, vararg args: Any?) = Log.d("Ripple", message)
 *     override fun info(message: String, vararg args: Any?) = Log.i("Ripple", message)
 *     override fun warn(message: String, vararg args: Any?) = Log.w("Ripple", message)
 *     override fun error(message: String, vararg args: Any?) = Log.e("Ripple", message)
 * }
 * ```
 */
interface LoggerAdapter {
    /**
     * Log debug message.
     * 
     * @param message Log message
     * @param args Optional message arguments
     */
    fun debug(message: String, vararg args: Any?)
    
    /**
     * Log info message.
     * 
     * @param message Log message
     * @param args Optional message arguments
     */
    fun info(message: String, vararg args: Any?)
    
    /**
     * Log warning message.
     * 
     * @param message Log message
     * @param args Optional message arguments
     */
    fun warn(message: String, vararg args: Any?)
    
    /**
     * Log error message.
     * 
     * @param message Log message
     * @param args Optional message arguments
     */
    fun error(message: String, vararg args: Any?)
}

/**
 * Log levels for controlling logger output verbosity.
 */
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR, NONE
}
