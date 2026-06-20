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
import com.tapsioss.ripple.core.*
import com.tapsioss.ripple.core.adapters.LogLevel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
                storageAdapter = RoomStorageAdapterFactory.create(this, ttl = 3600),
                loggerAdapter = AndroidLogAdapter(logLevel = LogLevel.DEBUG)
            )
        )

        rippleClient = AndroidRippleClient(this, config)
        rippleClient.init()
        rippleClient.screen(this)

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
        override fun getPayload() = buildJsonObject {
            put("button_name", buttonName)
            put("counter", counter)
            put("timestamp", System.currentTimeMillis())
        }
    }
    
    data class MetadataSet(val key: String, val value: String) : AppEvent() {
        override val name = "metadata_set"
        override fun getPayload() = buildJsonObject {
            put("key", key)
            put("value", value)
        }
    }
    
    object FlushTriggered : AppEvent() {
        override val name = "flush_triggered"
        override fun getPayload() = buildJsonObject {
            put("manual_flush", true)
        }
    }
    
    object MetadataCleared : AppEvent() {
        override val name = "metadata_cleared"
        override fun getPayload() = null
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
            .padding(16.dp)
            .systemBarsPadding(),
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
                trackPredefinedDemo(client)
                addLog("Tracked predefined product, cart, checkout, referral, and UI events")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Track Predefined Events")
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
                addLog("Anonymous ID: ${client.getAnonymousId()}")
                addLog("User ID: ${client.getUserId()}")
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

private fun trackPredefinedDemo(client: AndroidRippleClient<AppEvent, AppMetadata>) {
    val product = Product(
        productId = "sku-123",
        productTitle = "Ripple demo hoodie",
        price = Money(amount = 590_000, currency = "IRR"),
        category = Category(id = "apparel", title = "Apparel"),
        quantity = 1,
        position = 1,
        customProperties = buildJsonObject {
            put("screen", "sample_android")
            put("badge", "demo")
        }
    )
    val order = Order(
        orderId = "order-${System.currentTimeMillis()}",
        products = listOf(product),
        revenue = Money(amount = 590_000, currency = "IRR"),
        total = Money(amount = 590_000, currency = "IRR"),
        cartId = "cart-demo",
        paymentMethod = "card"
    )
    val checkout = Checkout(
        order = order,
        step = "shipping",
        checkoutId = "checkout-demo"
    )

    client.identify(
        userId = "test-user-123",
        traits = UserTraits(
            email = "demo@example.com",
            fullName = "Ripple Demo User",
            customProperties = buildJsonObject {
                put("sample", "android")
            }
        )
    )
    client.screen(ScreenPayload(title = "Android sample", pathname = "/android-sample"))
    client.clicked(ClickedPayload(elementId = "predefined_events_button", elementType = "button"))
    client.viewed(ViewedPayload(elementId = "demo_product_card", elementType = "product_card"))
    client.events.appStateChanged(
        AppStateChangedPayload(newState = AppState.FOREGROUND, previousState = AppState.BACKGROUND)
    )
    client.events.productsSearched(ProductsSearchedPayload(query = "hoodie"))
    client.events.productListViewed(
        ProductListViewedPayload(
            products = listOf(product),
            listId = "home_featured",
            category = Category(id = "featured", title = "Featured")
        )
    )
    client.events.productListFiltered(
        ProductListFilteredPayload(
            products = listOf(product),
            filters = listOf(Filter("size", "M")),
            sorts = listOf(Sort("price", "asc")),
            listId = "home_featured"
        )
    )
    client.events.productClicked(ProductPayload(product))
    client.events.productViewed(ProductPayload(product))
    client.events.productAddedToWishlist(ProductWishlistPayload(product, wishlistId = "wishlist-demo"))
    val cart = Cart(cartId = "cart-demo", products = listOf(product))
    client.events.productAddedToCart(CartModificationPayload(product, cart))
    client.events.cartViewed(CartPayload(cart))
    client.events.checkoutStarted(CheckoutPayload(checkout))
    client.events.orderCompleted(OrderPayload(order))
    client.events.paymentAuthorized(
        PaymentPayload(
            Payment(
                paymentId = "payment-demo",
                method = "card",
                value = Money(amount = 590_000, currency = "IRR"),
                orderId = order.orderId
            )
        )
    )
    client.events.promotionViewed(PromotionPayload(promotionId = "promo-demo", promotionTitle = "Demo campaign"))
    client.events.referralShared(
        ReferralPayload(
            referral = Referral(referralCode = "ANDROID-DEMO", referrerId = "test-user-123"),
            medium = "share_sheet",
            flow = "sample"
        )
    )
    client.events.challengeStarted(
        ChallengePayload(
            challenge = Challenge(challengeId = "challenge-demo", challengeTitle = "Sample onboarding")
        )
    )
}
