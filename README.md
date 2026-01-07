# Ripple Kotlin SDK

A high-performance event tracking SDK for Kotlin and Java applications.

[![Build](https://github.com/Tap30/ripple-kotlin/actions/workflows/build.yml/badge.svg)](https://github.com/Tap30/ripple-kotlin/actions/workflows/build.yml)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## Features

- 🚀 **High Performance**: O(1) queue operations with automatic batching
- 🔄 **Smart Retry Logic**: Exponential backoff with jitter, 4xx vs 5xx handling
- 🔒 **Thread-Safe**: All operations are thread-safe with mutex-protected flushes
- 💾 **Offline Support**: Events persisted and sent when connectivity returns
- 🌐 **Multi-Platform**: Android, Spring Boot, and reactive support
- 📘 **Type-Safe**: Full Kotlin type safety with Java interoperability
- 🔌 **Pluggable Adapters**: Customize HTTP, storage, and logging behavior
- ♻️ **Re-initializable**: Supports dispose/init cycles for lifecycle management
- 🔢 **Multi-Instance**: Run multiple client instances with different configs

## Download

### Repository Setup

<details open>
<summary><strong>Kotlin DSL (build.gradle.kts)</strong></summary>

```kotlin
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
        }
    }
}
```

</details>

<details>
<summary><strong>Groovy DSL (build.gradle)</strong></summary>

```groovy
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = "https://maven.pkg.github.com/Tap30/ripple-kotlin"
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```

</details>

### Core Modules

```kotlin
// Core functionality
implementation("io.github.tap30.ripple:core:1.0.0")

// Platform modules (lightweight, no adapters included)
implementation("io.github.tap30.ripple:android-core:1.0.0")
implementation("io.github.tap30.ripple:spring-core:1.0.0")
implementation("io.github.tap30.ripple:reactive-core:1.0.0")
```

### Adapter Modules (Optional)

Choose only the adapters you need:

<details open>
<summary><strong>Android Adapters</strong></summary>

```kotlin
// HTTP with OkHttp
implementation("io.github.tap30.ripple:android-adapters-okhttp:1.0.0")
// → OkHttpAdapter

// Storage with SharedPreferences  
implementation("io.github.tap30.ripple:android-adapters-storage-preferences:1.0.0")
// → SharedPreferencesAdapter

// Storage with Room Database
implementation("io.github.tap30.ripple:android-adapters-room:1.0.0") 
// → RoomStorageAdapter, RoomStorageAdapterFactory

// Android Logging
implementation("io.github.tap30.ripple:android-adapters-logging:1.0.0")
// → AndroidLogAdapter
```

</details>

<details>
<summary><strong>Spring Adapters</strong></summary>

```kotlin
// HTTP with WebClient (WebFlux)
implementation("io.github.tap30.ripple:spring-adapters-webflux:1.0.0")
// → WebClientAdapter

// File System Storage
implementation("io.github.tap30.ripple:spring-adapters-storage-file:1.0.0")
// → FileStorageAdapter

// SLF4J Logging
implementation("io.github.tap30.ripple:spring-adapters-logging:1.0.0")
// → Slf4jLoggerAdapter
```

</details>

<details>
<summary><strong>Reactive Adapters</strong></summary>

```kotlin
// Project Reactor Support
implementation("io.github.tap30.ripple:reactive-adapters-reactor:1.0.0")
// → ReactorAdapter (coming soon)
```

</details>

## Quick Start

### Android (Kotlin)

```kotlin
// Add dependencies
implementation("io.github.tap30.ripple:android-core:1.0.0")
implementation("io.github.tap30.ripple:android-adapters-okhttp:1.0.0")
implementation("io.github.tap30.ripple:android-adapters-room:1.0.0")

// Usage
import com.tapsioss.ripple.android.adapters.okhttp.OkHttpAdapter
import com.tapsioss.ripple.android.adapters.room.RoomStorageAdapterFactory

val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(),
        storageAdapter = RoomStorageAdapterFactory.create(context),
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
| `init()` | Initialize the client. Must be called before tracking. Can be called after dispose(). |
| `track(name, payload?, metadata?)` | Track an event with optional payload and metadata. |
| `setMetadata(key, value)` | Set global metadata attached to all events. |
| `getMetadata()` | Get all stored metadata as a shallow copy. |
| `getSessionId()` | Get the current session ID. |
| `removeMetadata(key)` | Remove a global metadata key. |
| `clearMetadata()` | Clear all global metadata. |
| `flush()` | Flush queued events asynchronously. |
| `flushSync()` | Flush queued events and wait for completion. |
| `getQueueSize()` | Get the number of queued events. |
| `dispose()` | Clean up resources. Persists unsent events. Supports re-initialization. |

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

## Multi-Instance Support

The SDK supports running multiple client instances simultaneously with different configurations:

```kotlin
// Analytics client
val analyticsClient = AndroidRippleClient(context, RippleConfig(
    apiKey = "analytics-key",
    endpoint = "https://analytics.example.com/events",
    adapters = AdapterConfig(...)
))

// Monitoring client  
val monitoringClient = AndroidRippleClient(context, RippleConfig(
    apiKey = "monitoring-key",
    endpoint = "https://monitoring.example.com/events",
    adapters = AdapterConfig(...)
))

analyticsClient.init()
monitoringClient.init()

// Track to different endpoints
analyticsClient.track("user_action", mapOf("action" to "click"))
monitoringClient.track("performance", mapOf("latency" to 150))
```

Each instance maintains its own:
- Event queue
- Metadata storage
- Session ID
- Flush scheduler

## Retry Behavior

The SDK handles HTTP errors intelligently:

| Status Code | Behavior |
|-------------|----------|
| 2xx | Success - clear storage |
| 4xx | No retry - persist events |
| 5xx | Retry with exponential backoff |
| Network error | Retry with exponential backoff |

Backoff formula: `delay = (1000ms × 2^attempt) + jitter(0-1000ms)`, max 30 seconds.

## Documentation

- [Detailed Documentation](AGENTS.md) - Complete API reference
- [GitHub Packages Setup](GITHUB_PACKAGES.md) - Authentication guide
- [Contributing](CONTRIBUTING.md) - Development guidelines
- [Changelog](CHANGELOG.md) - Release history

## License

MIT License - see [LICENSE](LICENSE) file for details.
