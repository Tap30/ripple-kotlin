package com.tapsioss.ripple.android.adapters

import android.content.Context
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * SharedPreferences-based storage adapter
 */
class SharedPreferencesAdapter(
    private val context: Context,
    private val prefsName: String = "ripple_events"
) : StorageAdapter {
    
    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun save(events: List<Event>) {
        val eventsJson = json.encodeToString(events)
        prefs.edit().putString("events", eventsJson).apply()
    }

    override suspend fun load(): List<Event> {
        val eventsJson = prefs.getString("events", null) ?: return emptyList()
        return try {
            json.decodeFromString<List<Event>>(eventsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun clear() {
        prefs.edit().remove("events").apply()
    }
}
