package com.tapsioss.ripple.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapsioss.ripple.android.AndroidRippleClient
import com.tapsioss.ripple.android.adapters.logging.AndroidLogAdapter
import com.tapsioss.ripple.android.adapters.okhttp.OkHttpAdapter
import com.tapsioss.ripple.android.adapters.room.RoomStorageAdapterFactory
import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.adapters.LogLevel

class MainActivity : ComponentActivity() {

    lateinit var rippleClient: AndroidRippleClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        rippleClient = AndroidRippleClient(config)
        rippleClient.init()

        setContent {
            MaterialTheme {
                RippleSampleScreen(rippleClient)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        rippleClient.dispose()
    }
}

@Composable
fun RippleSampleScreen(client: AndroidRippleClient) {
    var status by remember { mutableStateOf("Initialized") }
    var eventCounter by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = {
                eventCounter++
                client.track("test_event", mapOf("counter" to eventCounter))
                status = "Tracked event #$eventCounter"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Track Event")
        }

        Button(
            onClick = {
                client.setMetadata("user_id", "test-user-123")
                status = "Metadata set"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Metadata")
        }

        Button(
            onClick = {
                client.flush()
                status = "Flush triggered"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Flush")
        }

        Button(
            onClick = {
                client.clearMetadata()
                status = "Metadata cleared"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Metadata")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Status: $status")
    }
}
