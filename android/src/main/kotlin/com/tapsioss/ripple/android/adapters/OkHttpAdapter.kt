package com.tapsioss.ripple.android.adapters

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OkHttp-based HTTP adapter for reliable network requests on Android.
 * 
 * @param client OkHttp client instance, uses default if not provided
 */
class OkHttpAdapter(
    private val client: OkHttpClient = OkHttpClient()
) : HttpAdapter {
    
    /**
     * Send events to remote endpoint using OkHttp.
     * Automatically serializes events to JSON and handles network errors.
     * 
     * @param endpoint Target API endpoint URL
     * @param events List of events to send
     * @param headers HTTP headers to include
     * @param apiKeyHeader API key header name
     * @return HTTP response with success status and optional response data
     */
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
