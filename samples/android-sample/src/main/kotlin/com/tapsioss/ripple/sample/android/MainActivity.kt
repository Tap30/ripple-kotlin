package com.tapsioss.ripple.sample.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tapsioss.ripple.android.AndroidRippleClient
import com.tapsioss.ripple.android.adapters.logging.AndroidLogAdapter
import com.tapsioss.ripple.android.adapters.storage.SharedPreferencesAdapter
import com.tapsioss.ripple.android.adapters.okhttp.OkHttpAdapter
import com.tapsioss.ripple.android.adapters.room.RoomStorageAdapterFactory
import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.adapters.LogLevel

class MainActivity : AppCompatActivity() {

    private lateinit var rippleClient: AndroidRippleClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Ripple client with modular adapters
        val config = RippleConfig(
            apiKey = "your-api-key",
            endpoint = "https://api.example.com/events",
            adapters = AdapterConfig(
                httpAdapter = OkHttpAdapter(), // From ripple-android-okhttp module
                storageAdapter = RoomStorageAdapterFactory.create(this), // From ripple-android-room module
                loggerAdapter = AndroidLogAdapter(logLevel = LogLevel.DEBUG)
            )
        )

        rippleClient = AndroidRippleClient(this, config)

        rippleClient.init()
        // Track events
        rippleClient.setMetadata("userId", "user-123")
        rippleClient.track("app_opened", mapOf("screen" to "main"))
        rippleClient.track("button_clicked", mapOf("button" to "login"))
    }

    override fun onDestroy() {
        super.onDestroy()
        rippleClient.dispose()
    }
}
