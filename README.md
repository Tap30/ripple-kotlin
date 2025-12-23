# Ripple Kotlin SDK

A high-performance event tracking SDK for Kotlin and Java applications.

## Download

### GitHub Packages (Current)

Add to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
        credentials {
            username = "your_github_username"
            password = "your_github_token"
        }
    }
}

dependencies {
    // Android
    implementation("com.tapsioss.ripple:android:1.0.0")
    
    // Spring Boot
    implementation("com.tapsioss.ripple:spring:1.0.0")
    
    // Reactive
    implementation("com.tapsioss.ripple:reactive:1.0.0")
    
    // Core (for custom implementations)
    implementation("com.tapsioss.ripple:core:1.0.0")
}
```

### Maven Central (Coming Soon)

```kotlin
// Will be available without authentication
implementation("com.tapsioss.ripple:android:1.0.0")
```

## Quick Start

### Android

```kotlin
val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(),
        storageAdapter = SharedPreferencesAdapter(context),
        loggerAdapter = AndroidLogAdapter(LogLevel.INFO)
    )
)

val client = AndroidRippleClient(context, config)

// Initialize once
client.init()

// Track simple events
client.track("user_login")

// Track events with properties
client.track("purchase", mapOf(
    "product_id" to "abc123",
    "amount" to 29.99,
    "currency" to "USD"
))

// Set global metadata (attached to all events)
client.setMetadata("user_id", "12345")
client.setMetadata("app_version", "1.2.0")

// Track with event-specific metadata
client.track("page_view", 
    payload = mapOf("page" to "home"),
    metadata = mapOf("experiment" to "variant_a")
)

// Manual flush (events auto-flush based on config)
client.flush()

// Clean up when done
client.dispose()
```

### Spring Boot

```kotlin
@Configuration
class RippleConfig {
    
    @Bean
    fun rippleClient(): SpringRippleClient {
        val config = RippleConfig(
            apiKey = "your-api-key",
            endpoint = "https://api.example.com/events",
            flushInterval = 5000L, // 5 seconds
            maxBatchSize = 20,
            adapters = AdapterConfig(
                httpAdapter = WebClientAdapter(),
                storageAdapter = FileStorageAdapter(),
                loggerAdapter = Slf4jLoggerAdapter()
            )
        )
        
        return SpringRippleClient(config).apply {
            init()
            // Set application-wide metadata
            setMetadata("service", "user-service")
            setMetadata("environment", "production")
        }
    }
}

@Service
class UserService(private val rippleClient: SpringRippleClient) {
    
    fun createUser(user: User) {
        // Business logic...
        
        // Track user creation
        rippleClient.track("user_created", mapOf(
            "user_id" to user.id,
            "email" to user.email,
            "plan" to user.plan,
            "signup_method" to user.signupMethod
        ))
    }
    
    fun processPayment(payment: Payment) {
        // Set user context for this request
        rippleClient.setMetadata("user_id", payment.userId)
        
        // Track payment attempt
        rippleClient.track("payment_attempted", mapOf(
            "amount" to payment.amount,
            "currency" to payment.currency,
            "payment_method" to payment.method
        ))
        
        try {
            // Process payment...
            
            // Track success
            rippleClient.track("payment_completed", mapOf(
                "transaction_id" to payment.transactionId,
                "amount" to payment.amount
            ))
            
        } catch (e: Exception) {
            // Track failure with error details
            rippleClient.track("payment_failed", 
                payload = mapOf(
                    "amount" to payment.amount,
                    "error_code" to e.javaClass.simpleName
                ),
                metadata = mapOf(
                    "error_message" to e.message,
                    "stack_trace" to e.stackTraceToString()
                )
            )
        }
    }
}
```

### Reactive

```kotlin
val client = ReactiveRippleClient(config)

// Get event stream
client.getEventFlow()
    .collect { event ->
        println("Event tracked: ${event.name}")
    }

// Or with Project Reactor
client.getEventFlux()
    .subscribe { event ->
        println("Event tracked: ${event.name}")
    }
```

## Advanced Usage

### Custom Configuration

```kotlin
val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    apiKeyHeader = "Authorization", // Custom header name
    flushInterval = 10000L, // Flush every 10 seconds
    maxBatchSize = 50, // Send up to 50 events per batch
    maxRetries = 5, // Retry failed requests 5 times
    adapters = AdapterConfig(
        httpAdapter = CustomHttpAdapter(),
        storageAdapter = DatabaseStorageAdapter(),
        loggerAdapter = CustomLoggerAdapter(LogLevel.DEBUG)
    )
)
```

### Event Tracking Patterns

```kotlin
// E-commerce tracking
client.track("product_viewed", mapOf(
    "product_id" to "SKU123",
    "category" to "electronics",
    "price" to 299.99
))

client.track("cart_updated", mapOf(
    "action" to "add",
    "product_id" to "SKU123",
    "quantity" to 2,
    "cart_total" to 599.98
))

// User behavior tracking
client.track("feature_used", mapOf(
    "feature_name" to "dark_mode",
    "enabled" to true,
    "source" to "settings_menu"
))

// Performance tracking
client.track("api_call", mapOf(
    "endpoint" to "/api/users",
    "method" to "GET",
    "duration_ms" to 245,
    "status_code" to 200
))

// A/B testing
client.setMetadata("experiment_group", "control")
client.track("button_clicked", mapOf(
    "button_id" to "cta_primary",
    "page" to "landing"
))
```

### Metadata Management

```kotlin
// Set user context (persists across events)
client.setMetadata("user_id", "12345")
client.setMetadata("session_id", "abc-def-ghi")

// Set device/app context
client.setMetadata("device_type", "mobile")
client.setMetadata("app_version", "2.1.0")
client.setMetadata("platform", "android")

// Events will automatically include all set metadata
client.track("screen_view", mapOf("screen" to "profile"))
// ^ This event includes user_id, session_id, device_type, etc.

// Override metadata for specific events
client.track("error_occurred",
    payload = mapOf("error_type" to "network"),
    metadata = mapOf("user_id" to null) // Remove user_id for this event
)
```

### Manual Flushing

```kotlin
// Flush immediately (useful for critical events)
client.track("purchase_completed", purchaseData)
client.flush() // Ensures event is sent right away

// Flush on app lifecycle events
override fun onPause() {
    super.onPause()
    client.flush() // Send pending events before app goes to background
}

override fun onDestroy() {
    super.onDestroy()
    client.flush()
    client.dispose() // Clean up resources
}
```

## Features

- 🚀 **High Performance**: Efficient queue management with automatic batching
- 🔄 **Built-in Retry Logic**: Exponential backoff with jitter for failed requests
- 🔒 **Thread-Safe**: Concurrent operations without blocking your app
- 💾 **Offline Support**: Events are persisted and sent when connectivity returns
- 🌐 **Multi-Platform**: Android, Spring Boot, Reactive streams support
- 📘 **Type-Safe**: Full Kotlin type safety with Java interoperability
- 🔌 **Pluggable Adapters**: Customize HTTP, storage, and logging behavior
- ⚡ **No Coroutines Required**: Simple function calls, no async/await complexity

## Documentation

- [Detailed Documentation](AGENTS.md) - Complete API reference and examples
- [GitHub Packages Setup](GITHUB_PACKAGES.md) - How to authenticate and use packages
- [Contributing](CONTRIBUTING.md) - Development guidelines
- [Changelog](CHANGELOG.md) - Release history

## License

MIT License - see [LICENSE](LICENSE) file for details.
