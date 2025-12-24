package com.tapsioss.ripple.android.room

import android.content.Context
import androidx.room.Room
import com.tapsioss.ripple.core.adapters.StorageAdapter

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
     * @return Configured Room storage adapter
     */
    fun create(
        context: Context,
        databaseName: String = "ripple_events.db"
    ): StorageAdapter {
        val database = Room.databaseBuilder(
            context.applicationContext,
            RippleDatabase::class.java,
            databaseName
        )
        .fallbackToDestructiveMigration() // For simplicity in v1
        .build()
        
        return RoomStorageAdapter(database)
    }
    
    /**
     * Create a Room storage adapter with custom database configuration.
     * 
     * @param database Pre-configured Room database instance
     * @return Room storage adapter using the provided database
     */
    fun create(database: RippleDatabase): StorageAdapter {
        return RoomStorageAdapter(database)
    }
}
