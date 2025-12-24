package com.tapsioss.ripple.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager for global metadata attached to all events.
 * 
 * Metadata set here is automatically merged with event-specific metadata
 * when tracking events. Event-specific metadata takes precedence over
 * global metadata for the same keys.
 */
class MetadataManager {
    private val metadata = ConcurrentHashMap<String, Any>()

    /**
     * Set a metadata value.
     * Thread-safe.
     * 
     * @param key Metadata key
     * @param value Metadata value
     */
    fun set(key: String, value: Any) {
        metadata[key] = value
    }

    /**
     * Get a metadata value.
     * 
     * @param key Metadata key
     * @return Value or null if not found
     */
    fun get(key: String): Any? = metadata[key]

    /**
     * Get all metadata as an immutable copy.
     * Thread-safe.
     * 
     * @return Copy of all metadata
     */
    fun getAll(): Map<String, Any> = metadata.toMap()

    /**
     * Remove a metadata key.
     * 
     * @param key Metadata key to remove
     */
    fun remove(key: String) {
        metadata.remove(key)
    }

    /**
     * Check if metadata is empty.
     */
    fun isEmpty(): Boolean = metadata.isEmpty()

    /**
     * Clear all metadata.
     */
    fun clear() {
        metadata.clear()
    }
}
