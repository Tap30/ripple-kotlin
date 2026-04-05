package com.tapsioss.ripple.android.adapters.storage

import android.content.Context
import android.content.SharedPreferences
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class StorageData(val events: List<Event>, val savedAt: Long)

/**
 * SharedPreferences-based storage adapter for Android.
 * 
 * Persists events to SharedPreferences for offline support and retry
 * on app restart. Thread-safe through SharedPreferences' internal locking.
 * 
 * @param context Android context for accessing SharedPreferences
 * @param prefsName Name of the SharedPreferences file (default: "ripple_events")
 * @param ttl Time-to-live in milliseconds (default: null, no expiration)
 */
class SharedPreferencesAdapter(
    context: Context,
    prefsName: String = "ripple_events",
    private val ttl: Long? = null
) : StorageAdapter {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    companion object {
        private const val KEY_EVENTS = "pending_events"
    }

    override fun save(events: List<Event>) {
        if (events.isEmpty()) return
        
        try {
            val data = StorageData(events, System.currentTimeMillis())
            prefs.edit().putString(KEY_EVENTS, json.encodeToString(data)).apply()
        } catch (e: Exception) {
            // Silently fail - events will be lost but app won't crash
        }
    }

    override fun load(): List<Event> {
        return try {
            val jsonString = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
            val data = json.decodeFromString<StorageData>(jsonString)
            if (ttl != null && System.currentTimeMillis() - data.savedAt > ttl) {
                clear()
                return emptyList()
            }
            data.events
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun clear() {
        prefs.edit().remove(KEY_EVENTS).apply()
    }
}
