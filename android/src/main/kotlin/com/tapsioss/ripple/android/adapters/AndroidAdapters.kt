package com.tapsioss.ripple.android.adapters

import android.content.Context
import android.util.Log
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OkHttp-based HTTP adapter for Android
 */
class OkHttpAdapter(
    private val client: OkHttpClient = OkHttpClient()
) : HttpAdapter {
    
    override suspend fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse = withContext(Dispatchers.IO) {
        try {
            val json = Json.encodeToString(mapOf("events" to events))
            val body = json.toRequestBody("application/json".toMediaType())
            
            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(body)
            
            headers.forEach { (key, value) ->
                requestBuilder.addHeader(key, value)
            }
            
            val response = client.newCall(requestBuilder.build()).execute()
            
            HttpResponse(
                ok = response.isSuccessful,
                status = response.code,
                data = response.body?.string()
            )
        } catch (e: Exception) {
            HttpResponse(ok = false, status = -1, data = e.message)
        }
    }
}

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

/**
 * Android Log-based logger adapter
 */
class AndroidLogAdapter(
    private val tag: String = "Ripple",
    private val logLevel: LogLevel = LogLevel.WARN
) : LoggerAdapter {
    
    override fun debug(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.DEBUG) {
            Log.d(tag, formatMessage(message, *args))
        }
    }

    override fun info(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.INFO) {
            Log.i(tag, formatMessage(message, *args))
        }
    }

    override fun warn(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.WARN) {
            Log.w(tag, formatMessage(message, *args))
        }
    }

    override fun error(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.ERROR) {
            Log.e(tag, formatMessage(message, *args))
        }
    }

    private fun formatMessage(message: String, vararg args: Any?): String {
        return if (args.isNotEmpty()) {
            String.format(message, *args)
        } else {
            message
        }
    }
}
