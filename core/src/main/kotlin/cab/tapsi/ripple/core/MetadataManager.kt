package cab.tapsi.ripple.core

/**
 * Thread-safe manager for global metadata attached to all events.
 * Provides type-safe metadata management with generic support.
 * 
 * @param TMetadata The type definition for metadata
 */
class MetadataManager {
    private val lock = Any()
    private val metadata = mutableMapOf<String, Any>()

    @Volatile
    private var snapshot: Map<String, Any>? = null

    /**
     * Set a metadata value.
     */
    fun set(key: String, value: Any) {
        synchronized(lock) {
            metadata[key] = value
            snapshot = null
        }
    }

    /**
     * Get all metadata as a cached immutable snapshot.
     */
    fun getAll(): Map<String, Any> {
        snapshot?.let { return it }

        return synchronized(lock) {
            snapshot ?: metadata.toMap().also {
                snapshot = it
            }
        }
    }

    /**
     * Check if metadata is empty.
     */
    fun isEmpty(): Boolean = getAll().isEmpty()

    /**
     * Clear all metadata.
     */
    fun clear() {
        synchronized(lock) {
            metadata.clear()
            snapshot = null
        }
    }

}
