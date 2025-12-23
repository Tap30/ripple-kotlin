package com.tapsioss.ripple.core.adapters

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse

/**
 * HTTP adapter interface for sending events to remote endpoints.
 * Implement this interface to customize HTTP behavior.
 */
interface HttpAdapter {
    /**
     * Send events to the specified endpoint.
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
 * Implement this interface to customize event storage.
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
     * @return List of persisted events, empty if none found
     */
    fun load(): List<Event>
    
    /**
     * Clear all persisted events from storage.
     */
    fun clear()
}

/**
 * Logger adapter interface for SDK logging output.
 * Implement this interface to customize logging behavior.
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
