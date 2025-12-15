package com.tapsioss.ripple.sample.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tapsioss.ripple.android.AndroidRippleClient
import com.tapsioss.ripple.android.adapters.AndroidLogAdapter
import com.tapsioss.ripple.android.adapters.OkHttpAdapter
import com.tapsioss.ripple.android.adapters.SharedPreferencesAdapter
import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.adapters.LogLevel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var rippleClient: AndroidRippleClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Ripple client
        val config = RippleConfig(
            apiKey = "your-api-key",
            endpoint = "https://api.example.com/events",
            adapters = AdapterConfig(
                httpAdapter = OkHttpAdapter(),
                storageAdapter = SharedPreferencesAdapter(this),
                loggerAdapter = AndroidLogAdapter(logLevel = LogLevel.DEBUG)
            )
        )
        
        rippleClient = AndroidRippleClient(this, config)
        
        lifecycleScope.launch {
            rippleClient.init()
            
            // Track events
            rippleClient.setMetadata("userId", "user-123")
            rippleClient.track("app_opened", mapOf("screen" to "main"))
            rippleClient.track("button_clicked", mapOf("button" to "login"))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rippleClient.dispose()
    }
}
