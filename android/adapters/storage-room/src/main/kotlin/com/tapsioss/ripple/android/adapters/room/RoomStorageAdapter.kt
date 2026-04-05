package com.tapsioss.ripple.android.adapters.room

import androidx.room.*
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

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
    }

    override fun save(events: List<Event>) {
        if (events.isEmpty()) return
        
        runBlocking {
            val savedAt = System.currentTimeMillis()
            val entities = events.map { event ->
                EventEntity(
                    name = event.name,
                    payload = event.payload?.let { json.encodeToString(it) },
                    issuedAt = event.issuedAt,
                    metadata = event.metadata?.let { json.encodeToString(it) },
                    sessionId = event.sessionId,
                    platform = event.platform?.let { json.encodeToString(it) },
                    savedAt = savedAt
                )
            }
            database.eventDao().insertEvents(entities)
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
                    payload = entity.payload?.let { json.decodeFromString(it) },
                    issuedAt = entity.issuedAt,
                    metadata = entity.metadata?.let { json.decodeFromString(it) },
                    sessionId = entity.sessionId,
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
    val sessionId: String?,
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
    version = 2,
    exportSchema = false
)
abstract class RippleDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
}
