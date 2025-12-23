package com.tapsioss.ripple.android.adapters

import android.content.Context
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SharedPreferences-based storage adapter for persisting events on Android.
 * Events are stored as JSON and survive app restarts.
 * 
 * @param context Android context for accessing SharedPreferences
 * @param prefsName SharedPreferences file name
 */
class SharedPreferencesAdapter(
    private val context: Context,
    private val prefsName: String = "ripple_events"
) : StorageAdapter {
    
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Save events to SharedPreferences as JSON.
     * 
     * @param events List of events to persist
     */
    override fun save(events: List<Event>) {
        val eventsJson = json.encodeToString(events)
        prefs.edit().putString("events", eventsJson).apply()
    }

    /**
     * Load previously saved events from SharedPreferences.
     * 
     * @return List of events, empty if none found or parsing fails
     */
    override fun load(): List<Event> {
        val eventsJson = prefs.getString("events", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Event>>(eventsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clear all saved events from SharedPreferences.
     */
    override fun clear() {
        prefs.edit().remove("events").apply()
    }
}
