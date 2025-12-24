# Ripple Kotlin SDK

A high-performance event tracking SDK for Kotlin and Java applications.

[![Build](https://github.com/Tap30/ripple-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/Tap30/ripple-kotlin/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Features

- 🚀 **High Performance**: Efficient queue management with automatic batching
- 🔄 **Built-in Retry Logic**: Exponential backoff with jitter for failed requests
- 🔒 **Thread-Safe**: All operations are thread-safe, no synchronization needed
- 💾 **Offline Support**: Events are persisted and sent when connectivity returns
- 🌐 **Multi-Platform**: Android, Spring Boot, and pure Java support
- 📘 **Type-Safe**: Full Kotlin type safety with seamless Java interoperability
- 🔌 **Pluggable Adapters**: Customize HTTP, storage, and logging behavior
- ⚡ **No Coroutines Required**: Simple function calls, no async complexity

## Download

### GitHub Packages

Add to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USERNAME")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    // Android
    implementation("com.tapsioss.ripple:android:1.0.0")
    
    // Spring Boot
    implementation("com.tapsioss.ripple:spring:1.0.0")
    
    // Core (for custom implementations)
    implementation("com.tapsioss.ripple:core:1.0.0")
}
```

## Quick Start

### Android (Kotlin)

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

// Initialize once (typically in Application.onCreate)
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
client.track(
    name = "page_view",
    payload = mapOf("page" to "home"),
    metadata = mapOf("experiment" to "variant_a")
)

// Manual flush (events auto-flush based on config)
client.flush()

// Clean up when done
client.dispose()
```

### Spring Boot (Kotlin)

```kotlin
@Configuration
class RippleConfiguration {
    
    @Bean
    fun rippleClient(): SpringRippleClient {
        val config = RippleConfig(
            apiKey = "your-api-key",
            endpoint = "https://api.example.com/events",
            flushInterval = 5000L,
            maxBatchSize = 20,
            adapters = AdapterConfig(
                httpAdapter = WebClientAdapter(),
                storageAdapter = FileStorageAdapter(),
                loggerAdapter = Slf4jLoggerAdapter()
            )
        )
        
        return SpringRippleClient(config).apply {
            init()
            setMetadata("service", "user-service")
            setMetadata("environment", "production")
        }
    }
}

@Service
class UserService(private val rippleClient: SpringRippleClient) {
    
    fun createUser(user: User) {
        // Business logic...
        
        rippleClient.track("user_created", mapOf(
            "user_id" to user.id,
            "plan" to user.plan
        ))
    }
}
```

### Spring Boot (Java)

```java
@Configuration
public class RippleConfiguration {
    
    @Bean
    public SpringRippleClient rippleClient() {
        RippleConfig config = new RippleConfig(
            "your-api-key",
            "https://api.example.com/events",
            "X-API-Key",
            5000L,
            10,
            3,
            new AdapterConfig(
                new WebClientAdapter(),
                new FileStorageAdapter(),
                new Slf4jLoggerAdapter(LogLevel.INFO)
            )
        );
        
        SpringRippleClient client = new SpringRippleClient(config);
        client.init();
        client.setMetadata("service", "java-service");
        return client;
    }
}

@Service
public class UserService {
    
    private final SpringRippleClient rippleClient;
    
    public UserService(SpringRippleClient rippleClient) {
        this.rippleClient = rippleClient;
    }
    
    public void createUser(User user) {
        // Business logic...
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", user.getId());
        payload.put("plan", user.getPlan());
        
        rippleClient.track("user_created", payload, null);
    }
}
```

## API Reference

### RippleClient

| Method | Description |
|--------|-------------|
| `init()` | Initialize the client. Must be called before tracking. |
| `track(name, payload?, metadata?)` | Track an event with optional payload and metadata. |
| `setMetadata(key, value)` | Set global metadata attached to all events. |
| `removeMetadata(key)` | Remove a global metadata key. |
| `clearMetadata()` | Clear all global metadata. |
| `flush()` | Flush queued events asynchronously. |
| `flushSync()` | Flush queued events and wait for completion. |
| `getQueueSize()` | Get the number of queued events. |
| `dispose()` | Clean up resources. Persists unsent events. |

### RippleConfig

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `apiKey` | String | required | API authentication key |
| `endpoint` | String | required | API endpoint URL |
| `apiKeyHeader` | String | "X-API-Key" | Header name for API key |
| `flushInterval` | Long | 5000 | Auto-flush interval in milliseconds |
| `maxBatchSize` | Int | 10 | Maximum events per batch |
| `maxRetries` | Int | 3 | Maximum retry attempts |
| `adapters` | AdapterConfig | required | Platform adapters |

## Thread Safety

All public methods are thread-safe and can be called from any thread:

- `track()` is non-blocking and returns immediately
- `flush()` is non-blocking and submits work to background thread
- `flushSync()` blocks until completion (use for critical events)
- Multiple concurrent calls are handled gracefully

## Offline Support

Events are automatically persisted when:
- Network requests fail after all retries
- `dispose()` is called with events in queue

Persisted events are restored on next `init()` call.

## Documentation

- [Detailed Documentation](AGENTS.md) - Complete API reference
- [GitHub Packages Setup](GITHUB_PACKAGES.md) - Authentication guide
- [Contributing](CONTRIBUTING.md) - Development guidelines
- [Changelog](CHANGELOG.md) - Release history

## License

MIT License - see [LICENSE](LICENSE) file for details.
