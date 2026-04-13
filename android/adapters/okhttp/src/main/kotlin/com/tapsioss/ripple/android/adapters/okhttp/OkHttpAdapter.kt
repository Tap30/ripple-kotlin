package com.tapsioss.ripple.android.adapters.okhttp

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * OkHttp-based HTTP adapter for Android.
 * 
 * Provides reliable HTTP functionality using OkHttp with configurable
 * timeouts and automatic JSON serialization.
 * 
 * @param client OkHttp client instance. Uses a default client with 30-second
 *               timeouts if not provided.
 */
class OkHttpAdapter(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) : HttpAdapter {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        return try {
            val jsonBody = json.encodeToString(mapOf("events" to events))
            val body = jsonBody.toRequestBody("application/json".toMediaType())
            
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
            HttpResponse(
                ok = false, 
                status = -1, 
                data = e.message
            )
        }
    }
}
