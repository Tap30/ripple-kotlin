package cab.tapsi.ripple.android.adapters.room

import android.content.Context
import androidx.room.Room
import cab.tapsi.ripple.core.adapters.StorageAdapter

/**
 * Factory for creating Room storage adapter instances.
 * 
 * Provides a simple way to create and configure the Room database
 * for event storage in Android applications.
 * 
 * Example usage:
 * ```kotlin
 * val storageAdapter = RoomStorageAdapterFactory.create(context)
 * 
 * val config = RippleConfig(
 *     // ... other config
 *     adapters = AdapterConfig(
 *         storageAdapter = storageAdapter,
 *         // ... other adapters
 *     )
 * )
 * ```
 */
object RoomStorageAdapterFactory {
    
    /**
     * Create a Room storage adapter with default configuration.
     * 
     * @param context Android application context
     * @param databaseName Optional database name (default: "ripple_events.db")
     * @param ttl Time-to-live in milliseconds (default: null, no expiration)
     * @return Configured Room storage adapter
     */
    fun create(
        context: Context,
        databaseName: String = "ripple_events.db",
        ttl: Long? = null
    ): StorageAdapter {
        val database = Room.databaseBuilder(
            context = context.applicationContext,
            klass = RippleDatabase::class.java,
            name = databaseName
        )
        .fallbackToDestructiveMigration() //todo For simplicity in v1
        .build()
        
        return RoomStorageAdapter(database, ttl)
    }
    
    /**
     * Create a Room storage adapter with custom database configuration.
     * 
     * @param database Pre-configured Room database instance
     * @param ttl Time-to-live in milliseconds (default: null, no expiration)
     * @return Room storage adapter using the provided database
     */
    fun create(database: RippleDatabase, ttl: Long? = null): StorageAdapter {
        return RoomStorageAdapter(database, ttl)
    }
}
