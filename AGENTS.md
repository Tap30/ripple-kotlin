# Ripple Kotlin SDK - Detailed Documentation

A high-performance, scalable, and fault-tolerant event tracking SDK for Kotlin and Java applications.

## Features

- 🚀 **High Performance**: Efficient queue management with O(1) operations
- 📦 **Automatic Batching**: Configurable batch size and flush intervals  
- 🔄 **Retry Logic**: Exponential backoff with jitter
- 🔒 **Concurrency Safe**: Coroutine-based thread-safe operations
- 💾 **Multiple Storage Options**: SharedPreferences, File system, Database
- 🔌 **Pluggable Adapters**: Custom HTTP, storage, and logger implementations
- 📘 **Type-Safe**: Full Kotlin type safety with Java interoperability
- 🌐 **Multi-Platform**: Android, Spring Boot, Reactive streams support
- ✅ **No Event Loss**: Events preserved during failures and retries
- 📋 **Event Ordering**: FIFO order maintained across all scenarios

## Modules

- **core**: Shared core logic and interfaces
- **android**: Android-specific implementation with lifecycle awareness
- **spring**: Spring Boot integration with auto-configuration
- **reactive**: Reactive streams support with Kotlin Flow and Project Reactor

## Quick Start

### Android

```kotlin
// Add to build.gradle.kts
implementation("com.tapsioss.ripple:android:1.0.0")

// Initialize client
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

// Track events
lifecycleScope.launch {
    client.init()
    client.track("user_login", mapOf("method" to "google"))
    client.flush()
}
```

### Spring Boot

```kotlin
// Add to build.gradle.kts
implementation("com.tapsioss.ripple:spring:1.0.0")

// Configuration
@Bean
fun rippleClient(): SpringRippleClient {
    val config = RippleConfig(
        apiKey = "your-api-key", 
        endpoint = "https://api.example.com/events",
        adapters = AdapterConfig(
            httpAdapter = WebClientAdapter(),
            storageAdapter = FileStorageAdapter(),
            loggerAdapter = Slf4jLoggerAdapter()
        )
    )
    return SpringRippleClient(config)
}

// Usage
@Service
class UserService(private val rippleClient: SpringRippleClient) {
    
    suspend fun createUser(user: User) {
        // Business logic...
        
        rippleClient.track("user_created", mapOf(
            "userId" to user.id,
            "plan" to user.plan
        ))
    }
}
```

### Reactive

```kotlin
// Add to build.gradle.kts
implementation("com.tapsioss.ripple:reactive:1.0.0")

// Usage with Flow
val client = ReactiveRippleClient(config)

client.getEventFlow()
    .collect { event ->
        println("Event tracked: ${event.name}")
    }

// Usage with Reactor
client.getEventFlux()
    .subscribe { event ->
        println("Event tracked: ${event.name}")
    }
```

## Configuration

```kotlin
data class RippleConfig(
    val apiKey: String,                    // Required: API authentication key
    val endpoint: String,                  // Required: API endpoint URL
    val apiKeyHeader: String = "X-API-Key", // Optional: Header name for API key
    val flushInterval: Long = 5000L,       // Optional: Auto-flush interval (ms)
    val maxBatchSize: Int = 10,            // Optional: Max events per batch
    val maxRetries: Int = 3,               // Optional: Max retry attempts
    val adapters: AdapterConfig            // Required: Platform adapters
)
```

## Custom Adapters

### HTTP Adapter

```kotlin
class CustomHttpAdapter : HttpAdapter {
    override suspend fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        // Custom HTTP implementation
        return HttpResponse(ok = true, status = 200)
    }
}
```

### Storage Adapter

```kotlin
class RedisStorageAdapter : StorageAdapter {
    override suspend fun save(events: List<Event>) {
        // Save to Redis
    }
    
    override suspend fun load(): List<Event> {
        // Load from Redis
        return emptyList()
    }
    
    override suspend fun clear() {
        // Clear Redis storage
    }
}
```

## Java Interoperability

The SDK is fully compatible with Java:

```java
// Java usage example
RippleConfig config = new RippleConfig(
    "your-api-key",
    "https://api.example.com/events",
    "X-API-Key",
    5000L,
    10,
    3,
    new AdapterConfig(
        new OkHttpAdapter(),
        new SharedPreferencesAdapter(context),
        new AndroidLogAdapter(LogLevel.INFO)
    )
);

AndroidRippleClient client = new AndroidRippleClient(context, config);

// Track events (using coroutines from Java)
CompletableFuture<Void> future = client.track("event_name", 
    Map.of("key", "value"), null);
```

## Architecture

The SDK follows a modular architecture with maximum code sharing:

```
ripple-kotlin/
├── core/                    # Shared core logic
│   ├── RippleClient         # Abstract client
│   ├── Dispatcher           # Queue management & retry logic
│   ├── MetadataManager      # Metadata handling
│   └── adapters/            # Adapter interfaces
├── android/                 # Android-specific implementation
│   ├── AndroidRippleClient  # Android client
│   ├── SessionManager       # Session lifecycle
│   └── adapters/            # Android adapters
├── spring/                  # Spring Boot integration
│   ├── SpringRippleClient   # Spring client
│   └── adapters/            # Spring adapters
└── reactive/                # Reactive streams support
    ├── ReactiveRippleClient # Reactive client
    └── Flow/Flux support    # Kotlin Flow & Reactor
```

## Concurrency Safety

- **Coroutine-based**: All async operations use Kotlin coroutines
- **Mutex Protection**: Thread-safe flush operations
- **Structured Concurrency**: Proper cancellation and error handling
- **No Event Loss**: Events preserved during concurrent operations

## Testing

Run tests for all modules:

```bash
./gradlew test
```

Run tests for specific module:

```bash
./gradlew :android:test
./gradlew :spring:test
./gradlew :core:test
```

## Building

Build all modules:

```bash
./gradlew build
```

Build specific module:

```bash
./gradlew :android:build
./gradlew :spring:build
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make changes with tests
4. Run `./gradlew check`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
