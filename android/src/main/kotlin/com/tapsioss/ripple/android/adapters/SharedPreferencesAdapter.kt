package com.tapsioss.ripple.android.adapters

import android.content.Context
import android.content.SharedPreferences
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SharedPreferences-based storage adapter for Android.
 * 
 * Persists events to SharedPreferences for offline support and retry
 * on app restart. Thread-safe through SharedPreferences' internal locking.
 * 
 * @param context Android context for accessing SharedPreferences
 * @param prefsName Name of the SharedPreferences file (default: "ripple_events")
 */
class SharedPreferencesAdapter(
    context: Context,
    prefsName: String = "ripple_events"
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
            val jsonString = json.encodeToString(events)
            prefs.edit().putString(KEY_EVENTS, jsonString).apply()
        } catch (e: Exception) {
            // Silently fail - events will be lost but app won't crash
        }
    }

    override fun load(): List<Event> {
        return try {
            val jsonString = prefs.getString(KEY_EVENTS, null) ?: return emptyList()
            json.decodeFromString<List<Event>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun clear() {
        prefs.edit().remove(KEY_EVENTS).apply()
    }
}
