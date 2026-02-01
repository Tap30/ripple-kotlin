package com.tapsioss.ripple.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe manager for global metadata attached to all events.
 * Provides type-safe metadata management with generic support.
 * 
 * @param TMetadata The type definition for metadata
 */
class MetadataManager {
    private val metadata = ConcurrentHashMap<String, Any>()

    /**
     * Set a metadata value.
     */
    fun set(key: String, value: Any) {
        metadata[key] = value
    }

    /**
     * Get all metadata as a copy.
     */
    fun getAll(): Map<String, Any> = metadata.toMap()

    /**
     * Check if metadata is empty.
     */
    fun isEmpty(): Boolean = metadata.isEmpty()

    /**
     * Remove a metadata key.
     */
    fun remove(key: String) {
        metadata.remove(key)
    }

    /**
     * Clear all metadata.
     */
    fun clear() {
        metadata.clear()
    }

    /**
     * Merge shared metadata with event-specific metadata.
     * Event-specific metadata takes precedence.
     * 
     * @param eventMetadata Event-specific metadata
     * @return Merged metadata or null if both are empty
     */
    fun merge(eventMetadata: Map<String, Any>?): Map<String, Any>? {
        if (isEmpty() && eventMetadata.isNullOrEmpty()) return null
        if (isEmpty()) return eventMetadata
        if (eventMetadata.isNullOrEmpty()) return getAll()
        return getAll() + eventMetadata
    }
}
