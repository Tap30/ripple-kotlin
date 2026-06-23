package cab.tapsi.ripple.android.adapters.room

import androidx.room.*
import cab.tapsi.ripple.core.AnySerializer
import cab.tapsi.ripple.core.Event
import cab.tapsi.ripple.core.Platform
import cab.tapsi.ripple.core.adapters.StorageAdapter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Room database storage adapter for Android.
 * 
 * Provides high-performance, structured storage for events using SQLite
 * with Room's async capabilities and type safety.
 * 
 * @param database Room database instance
 * @param ttl Time-to-live in milliseconds (default: null, no expiration)
 */
class RoomStorageAdapter(
    private val database: RippleDatabase,
    private val ttl: Long? = null
) : StorageAdapter {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "_type"
    }

    private val metadataSerializer = MapSerializer(String.serializer(), AnySerializer)



    override fun save(events: List<Event>) {
        if (events.isEmpty()) return

        runBlocking {
            val savedAt = System.currentTimeMillis()
            val entities = events.map { event ->
                EventEntity(
                    name = event.name,
                    payload = event.payload?.let { json.encodeToString(it) },
                    issuedAt = event.issuedAt,
//                    metadata = event.metadata?.let { metadataToJson(it) },
                    metadata = event.metadata?.let { json.encodeToString(metadataSerializer, it) },
                    platform = event.platform?.let { json.encodeToString(it) },
                    savedAt = savedAt
                )
            }
            database.eventDao().insertEvents(entities)
        }
    }

    private fun metadataToJson(metadata: Map<String, Any>): String {
        val jsonObject = JsonObject(metadata.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is Map<*, *> -> metadataMapToJsonElement(value)
                is List<*> -> JsonArray(value.map { metadataValueToJsonElement(it) })
                null -> JsonNull
                else -> JsonPrimitive(value.toString())
            }
        })
        return json.encodeToString(jsonObject)
    }

    private fun metadataMapToJsonElement(map: Map<*, *>): JsonElement {
        return JsonObject(map.mapKeys { it.key.toString() }.mapValues { (_, value) ->
            metadataValueToJsonElement(value)
        })
    }

    private fun metadataValueToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> metadataMapToJsonElement(value)
            is List<*> -> JsonArray(value.map { metadataValueToJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
    }

    override fun load(): List<Event> {
        return runBlocking {
            if (ttl != null) {
                val cutoff = System.currentTimeMillis() - ttl
                database.eventDao().deleteExpiredEvents(cutoff)
            }
            database.eventDao().getAllEvents().map { entity ->
                Event(
                    name = entity.name,
                    payload = entity.payload?.let { json.decodeFromString<JsonObject>(it) },
                    issuedAt = entity.issuedAt,
                    metadata = entity.metadata?.let { json.decodeFromString(it) },
                    platform = entity.platform?.let { json.decodeFromString(it) }
                )
            }
        }
    }

    override fun clear() {
        runBlocking {
            database.eventDao().deleteAllEvents()
        }
    }
}

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val payload: String?,
    val issuedAt: Long,
    val metadata: String?,
    val platform: String?,
    val savedAt: Long = System.currentTimeMillis()
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY issuedAt ASC")
    suspend fun getAllEvents(): List<EventEntity>
    
    @Insert
    suspend fun insertEvents(events: List<EventEntity>)
    
    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()
    
    @Query("DELETE FROM events WHERE savedAt < :cutoff")
    suspend fun deleteExpiredEvents(cutoff: Long)
    
    @Query("SELECT COUNT(*) FROM events")
    suspend fun getEventCount(): Int
}

@Database(
    entities = [EventEntity::class],
    version = 3,
    exportSchema = false
)
abstract class RippleDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
