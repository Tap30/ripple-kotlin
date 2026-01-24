package com.tapsioss.ripple.sample.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapsioss.ripple.android.AndroidRippleClient
import com.tapsioss.ripple.android.adapters.logging.AndroidLogAdapter
import com.tapsioss.ripple.android.adapters.okhttp.OkHttpAdapter
import com.tapsioss.ripple.android.adapters.room.RoomStorageAdapterFactory
import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.RippleEvent
import com.tapsioss.ripple.core.RippleMetadata
import com.tapsioss.ripple.core.adapters.LogLevel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    lateinit var rippleClient: AndroidRippleClient<AppEvent, AppMetadata>

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

// Define type-safe events
sealed class AppEvent : RippleEvent {
    data class ButtonClick(val buttonName: String, val counter: Int) : AppEvent() {
        override val name = "button_click"
        override fun toPayload() = mapOf(
            "button_name" to buttonName,
            "counter" to counter,
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    data class MetadataSet(val key: String, val value: String) : AppEvent() {
        override val name = "metadata_set"
        override fun toPayload() = mapOf(
            "key" to key,
            "value" to value
        )
    }
    
    object FlushTriggered : AppEvent() {
        override val name = "flush_triggered"
        override fun toPayload() = mapOf(
            "manual_flush" to true
        )
    }
    
    object MetadataCleared : AppEvent() {
        override val name = "metadata_cleared"
        override fun toPayload() = null
    }
}

// Define type-safe metadata
data class AppMetadata(
    val userId: String? = null,
    val sessionType: String? = null,
    val appVersion: String? = null
) : RippleMetadata {
    override fun toMap() = buildMap {
        userId?.let { put("user_id", it) }
        sessionType?.let { put("session_type", it) }
        appVersion?.let { put("app_version", it) }
    }
}

@Composable
fun RippleSampleScreen(client: AndroidRippleClient<AppEvent, AppMetadata>) {
    var eventCounter by remember { mutableIntStateOf(0) }
    val logs = remember { mutableStateListOf<String>() }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new log is added
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }
    
    fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        logs.add("[$timestamp] $message")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Buttons section
        Button(
            onClick = {
                eventCounter++
                val event = AppEvent.ButtonClick("track_event", eventCounter)
                client.track(event)
                addLog("Tracked: ${event.name} with counter=$eventCounter")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Track Event (#$eventCounter)")
        }

        Button(
            onClick = {
                val metadata = AppMetadata(
                    userId = "test-user-123",
                    sessionType = "demo",
                    appVersion = "1.0.0"
                )
                client.setMetadata(metadata)
                client.track(AppEvent.MetadataSet("user_metadata", "set"))
                addLog("Set metadata: userId=test-user-123, sessionType=demo, appVersion=1.0.0")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Set Metadata")
        }

        Button(
            onClick = {
                client.track(AppEvent.FlushTriggered)
                client.flush()
                addLog("Manual flush triggered - events sent to server")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Flush Events")
        }

        Button(
            onClick = {
                client.clearMetadata()
                client.track(AppEvent.MetadataCleared)
                addLog("All metadata cleared")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Metadata")
        }
        
        Button(
            onClick = {
                addLog("Queue size: ${client.getQueueSize()} events")
                addLog("Session ID: ${client.getSessionId()}")
                addLog("Current metadata: ${client.getMetadata()}")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Status")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Logs section
        Text(
            text = "Event Logs:",
            style = MaterialTheme.typography.titleMedium
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (logs.isEmpty()) {
                    item {
                        Text(
                            text = "No events tracked yet. Click buttons above to start tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(logs) { log ->
                        Text(
                            text = log,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        // Clear logs button
        Button(
            onClick = { logs.clear() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Clear Logs")
        }
    }
}
