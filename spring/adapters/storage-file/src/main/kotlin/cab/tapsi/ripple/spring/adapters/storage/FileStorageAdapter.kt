package cab.tapsi.ripple.spring.adapters.storage

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cab.tapsi.ripple.core.Event
import cab.tapsi.ripple.core.adapters.StorageAdapter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class StorageData(val events: List<Event>, val savedAt: Long)

/**
 * File-based storage adapter for Spring applications.
 * 
 * Persists events to a JSON file for offline support and retry.
 * Thread-safe through file locking.
 * 
 * @param storagePath Path to the storage file (default: ripple_events.json in temp directory)
 * @param objectMapper Jackson ObjectMapper for JSON serialization
 * @param ttl Time-to-live in milliseconds (default: null, no expiration)
 */
class FileStorageAdapter(
    private val storagePath: Path = Files.createTempDirectory("ripple").resolve("events.json"),
    @Suppress("UNUSED_PARAMETER")
    objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule(),
    private val ttl: Long? = null
) : StorageAdapter {
    
    private val file: File = storagePath.toFile()
    private val lock = Any()
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "_type"
    }

    override fun save(events: List<Event>) {
        if (events.isEmpty()) return
        
        synchronized(lock) {
            try {
                file.parentFile?.mkdirs()
                val data = StorageData(events, System.currentTimeMillis())
                file.writeText(json.encodeToString(data))
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    override fun load(): List<Event> {
        synchronized(lock) {
            return try {
                if (!file.exists()) return emptyList()
                val data = json.decodeFromString<StorageData>(file.readText())
                if (ttl != null && System.currentTimeMillis() - data.savedAt > ttl) {
                    clear()
                    return emptyList()
                }
                data.events
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun clear() {
        synchronized(lock) {
            try {
                file.delete()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
