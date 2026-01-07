package com.tapsioss.ripple.sample.android

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tapsioss.ripple.android.AndroidRippleClient
import com.tapsioss.ripple.android.adapters.logging.AndroidLogAdapter
import com.tapsioss.ripple.android.adapters.okhttp.OkHttpAdapter
import com.tapsioss.ripple.android.adapters.room.RoomStorageAdapterFactory
import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.adapters.LogLevel

class MainActivity : AppCompatActivity() {

    lateinit var rippleClient: AndroidRippleClient
    private lateinit var tvStatus: TextView
    private var eventCounter = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)

        // Use 10.0.2.2 for emulator, or Mac's IP for physical device
        val endpoint = intent.getStringExtra("endpoint") ?: "${TestConfig.SERVER_URL}/events"

        val config = RippleConfig(
            apiKey = "test-api-key",
            endpoint = endpoint,
            flushInterval = 5000L,
            maxBatchSize = 10,
            adapters = AdapterConfig(
                httpAdapter = OkHttpAdapter(),
                storageAdapter = RoomStorageAdapterFactory.create(this),
                loggerAdapter = AndroidLogAdapter(logLevel = LogLevel.DEBUG)
            )
        )

        rippleClient = AndroidRippleClient(this, config)
        rippleClient.init()

        setupButtons()
        updateStatus("Initialized")
    }

    private fun setupButtons() {
        findViewById<Button>(R.id.btnTrackEvent).setOnClickListener {
            eventCounter++
            rippleClient.track("test_event", mapOf("counter" to eventCounter))
            updateStatus("Tracked event #$eventCounter")
        }

        findViewById<Button>(R.id.btnSetMetadata).setOnClickListener {
            rippleClient.setMetadata("user_id", "test-user-123")
            updateStatus("Metadata set")
        }

        findViewById<Button>(R.id.btnFlush).setOnClickListener {
            rippleClient.flush()
            updateStatus("Flush triggered")
        }

        findViewById<Button>(R.id.btnClearMetadata).setOnClickListener {
            rippleClient.clearMetadata()
            updateStatus("Metadata cleared")
        }
    }

    private fun updateStatus(message: String) {
        tvStatus.text = "Status: $message"
    }

    override fun onDestroy() {
        super.onDestroy()
        rippleClient.dispose()
    }
}
