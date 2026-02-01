package com.tapsioss.ripple.sample.android

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * Helper to interact with the local test server for E2E verification.
 */
class TestServerClient(private val baseUrl: String = TestConfig.SERVER_URL) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun getEvents(): List<Map<String, Any>> {
        val request = Request.Builder().url("$baseUrl/events").get().build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: "[]"
        val jsonArray = JSONArray(body)
        return (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            obj.keys().asSequence().associateWith { obj.get(it) }
        }
    }

    fun clearEvents() {
        val request = Request.Builder()
            .url("$baseUrl/events")
            .delete()
            .build()
        client.newCall(request).execute()
    }

    fun setServerError(enabled: Boolean) {
        val json = """{"error": $enabled}"""
        val request = Request.Builder()
            .url("$baseUrl/config")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute()
    }
}
