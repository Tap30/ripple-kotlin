package com.tapsioss.ripple.core

/**
 * Manage global metadata attached to all events
 */
class MetadataManager {
    private val metadata = mutableMapOf<String, Any>()

    /**
     * Set metadata value
     */
    fun set(key: String, value: Any) {
        metadata[key] = value
    }

    /**
     * Get metadata value
     */
    fun get(key: String): Any? = metadata[key]

    /**
     * Get all metadata
     */
    fun getAll(): Map<String, Any> = metadata.toMap()

    /**
     * Check if metadata is empty
     */
    fun isEmpty(): Boolean = metadata.isEmpty()

    /**
     * Clear all metadata
     */
    fun clear() {
        metadata.clear()
    }
}
