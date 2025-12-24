# Ripple Kotlin SDK - Technical Documentation

A high-performance, scalable, and fault-tolerant event tracking SDK for Kotlin and Java applications with multi-platform support.

## Architecture Overview

The SDK follows a modular architecture with maximum code sharing and platform-specific optimizations:

```
ripple-kotlin/
├── core/                    # Shared core logic (thread-safe, coroutine-based)
│   ├── RippleClient         # Abstract client with lifecycle management
│   ├── Dispatcher           # Queue management, batching, retry logic
│   ├── MetadataManager      # Thread-safe metadata handling
│   └── adapters/            # Pluggable adapter interfaces
├── android/                 # Android-specific implementation
│   ├── AndroidRippleClient  # Android client with session management
│   └── adapters/            # OkHttp, SharedPreferences, Android Log
├── spring/                  # Spring Boot integration
│   ├── SpringRippleClient   # Spring client for server applications
│   └── adapters/            # WebClient, File storage, SLF4J
├── reactive/                # Reactive streams support
│   └── ReactiveRippleClient # Kotlin Flow and Project Reactor support
└── samples/                 # Example implementations
    ├── android-sample/      # Android app example
    ├── spring-sample/       # Spring Boot example
    └── spring-java-sample/  # Java Spring Boot example
```

## Core Features

### 🚀 High Performance
- **O(1) Queue Operations**: ConcurrentLinkedQueue for thread-safe event queuing
- **Automatic Batching**: Configurable batch size (default: 10 events)
- **Non-blocking API**: All public methods return immediately
- **Background Processing**: Dedicated thread pool for HTTP operations

### 🔄 Fault Tolerance
- **Exponential Backoff**: Retry failed requests with jitter (max 30s delay)
- **Offline Persistence**: Events saved to storage when network fails
- **Graceful Degradation**: Continues queuing events during outages
- **Automatic Recovery**: Restores persisted events on next initialization

### 🔒 Thread Safety
- **Coroutine-based**: All async operations use Kotlin coroutines
- **Concurrent Collections**: Thread-safe data structures throughout
- **Atomic Operations**: Lock-free operations where possible
- **Synchronized Blocks**: Minimal locking for initialization/disposal

### 📦 Pluggable Architecture
- **HTTP Adapters**: OkHttp (Android), WebClient (Spring), custom implementations
- **Storage Adapters**: SharedPreferences, File system, database, custom
- **Logger Adapters**: Android Log, SLF4J, custom logging frameworks

## Core Module

### RippleClient (Abstract Base)

The foundation class providing lifecycle management and event tracking:

```kotlin
abstract class RippleClient(protected val config: RippleConfig) {
    // Lifecycle methods
    fun init()                                    // Initialize client, restore events
    fun dispose()                                 // Clean shutdown, persist events
    
    // Event tracking
    fun track(name: String, payload: Map<String, Any>? = null, metadata: Map<String, Any>? = null)
    
    // Metadata management
    fun setMetadata(key: String, value: Any)      // Global metadata for all events
    fun removeMetadata(key: String)
    fun clearMetadata()
    
    // Manual flushing
    fun flush()                                   // Non-blocking flush
    fun flushSync()                              // Blocking flush
    fun getQueueSize(): Int                      // Current queue size
    
    // Platform-specific implementations
    protected abstract fun getSessionId(): String?
    protected abstract fun getPlatform(): Platform?
}
```

**Key Implementation Details:**
- **Initialization**: Idempotent, thread-safe, restores persisted events
- **Event Creation**: Automatic timestamp, session ID, platform detection
- **Metadata Merging**: Global + event-specific metadata (event takes precedence)
- **Error Handling**: IllegalStateException if not initialized

### Dispatcher (Queue Management)

Handles event queuing, batching, and HTTP delivery with retry logic:

```kotlin
class Dispatcher(
    private val config: DispatcherConfig,
    private val httpAdapter: HttpAdapter,
    private val storageAdapter: StorageAdapter,
    private val loggerAdapter: LoggerAdapter?
) {
    // Queue operations
    fun enqueue(event: Event)                    // Add event to queue
    fun flush()                                  // Non-blocking flush
    fun flushSync()                             // Blocking flush
    
    // Lifecycle
    fun restore()                               // Restore persisted events
    fun startScheduledFlush()                   // Start auto-flush timer
    fun dispose()                               // Clean shutdown
    
    fun getQueueSize(): Int                     // Current queue size
}
```

**Implementation Details:**
- **Queue**: ConcurrentLinkedQueue for thread-safe operations
- **Batching**: Automatic flush when maxBatchSize reached
- **Retry Logic**: Exponential backoff with jitter (1s → 2s → 4s → ... max 30s)
- **Concurrency**: ReentrantLock prevents concurrent flushes
- **Scheduled Flush**: ScheduledExecutorService for periodic flushing
- **Error Recovery**: Failed events re-queued and persisted

### MetadataManager (Thread-safe Storage)

Manages global metadata attached to all events:

```kotlin
class MetadataManager {
    fun set(key: String, value: Any)            // Set metadata value
    fun remove(key: String)                     // Remove metadata key
    fun clear()                                 // Clear all metadata
    fun getAll(): Map<String, Any>              // Get all metadata (copy)
}
```

**Implementation**: ConcurrentHashMap for thread-safe operations

### Adapter Interfaces

#### HttpAdapter
```kotlin
interface HttpAdapter {
    fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse
}
```

#### StorageAdapter
```kotlin
interface StorageAdapter {
    fun save(events: List<Event>)               // Persist events
    fun load(): List<Event>                     // Load persisted events
    fun clear()                                 // Clear storage
}
```

#### LoggerAdapter
```kotlin
interface LoggerAdapter {
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}
```

## Platform Modules

### Android Module

**AndroidRippleClient**: Android-optimized implementation with lifecycle awareness

```kotlin
class AndroidRippleClient(
    private val context: Context,
    config: RippleConfig
) : RippleClient(config)
```

**Key Features:**
- **Session Management**: UUID-based session tracking
- **Platform Detection**: Automatic Android device info (OS, version, model, manufacturer)
- **Context Integration**: Uses Android Context for storage and system info

**Adapters:**
- **OkHttpAdapter**: HTTP client with 30s timeouts, JSON serialization
- **SharedPreferencesAdapter**: Persistent storage using Android SharedPreferences
- **AndroidLogAdapter**: Logging via Android Log with configurable levels

### Spring Module

**SpringRippleClient**: Spring Boot integration for server applications

```kotlin
class SpringRippleClient(config: RippleConfig) : RippleClient(config)
```

**Key Features:**
- **Bean Integration**: Designed as singleton Spring bean
- **Server Platform**: JVM system properties for platform detection
- **Thread Safety**: Safe for concurrent use in web applications

**Adapters:**
- **WebClientAdapter**: Reactive HTTP client using Spring WebFlux
- **FileStorageAdapter**: File system persistence for event storage
- **Slf4jLoggerAdapter**: Integration with SLF4J logging framework

### Reactive Module

**ReactiveRippleClient**: Reactive streams support with Kotlin Flow and Project Reactor

```kotlin
class ReactiveRippleClient(config: RippleConfig) : RippleClient(config) {
    suspend fun trackReactive(name: String, payload: Map<String, Any>? = null, metadata: Map<String, Any>? = null)
    fun getEventFlow(): Flow<Event>             // Kotlin Flow stream
    fun getEventFlux(): Flux<Event>             // Project Reactor stream
}
```

**Key Features:**
- **Kotlin Flow**: Native coroutine-based reactive streams
- **Project Reactor**: Integration with Reactor Flux
- **Event Streaming**: Real-time event emission for monitoring/analytics

## Configuration

### RippleConfig
```kotlin
data class RippleConfig(
    val apiKey: String,                         // Required: API authentication
    val endpoint: String,                       // Required: API endpoint URL
    val apiKeyHeader: String = "X-API-Key",     // Header name for API key
    val flushInterval: Long = 5000L,            // Auto-flush interval (ms)
    val maxBatchSize: Int = 10,                 // Max events per batch
    val maxRetries: Int = 3,                    // Max retry attempts
    val adapters: AdapterConfig                 // Platform adapters
)
```

### AdapterConfig
```kotlin
data class AdapterConfig(
    val httpAdapter: HttpAdapter,               // Required: HTTP implementation
    val storageAdapter: StorageAdapter,         // Required: Storage implementation
    val loggerAdapter: LoggerAdapter? = null    // Optional: Logging implementation
)
```

## Usage Examples

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
client.init()

// Set global metadata
client.setMetadata("user_id", "12345")
client.setMetadata("app_version", "1.2.0")

// Track events
client.track("user_login", mapOf("method" to "google"))
client.track("purchase", mapOf(
    "product_id" to "abc123",
    "amount" to 29.99,
    "currency" to "USD"
))

// Manual flush and cleanup
client.flush()
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
                new Slf4jLoggerAdapter()
            )
        );
        
        SpringRippleClient client = new SpringRippleClient(config);
        client.init();
        client.setMetadata("service", "java-service");
        return client;
    }
}
```

### Reactive (Kotlin)
```kotlin
val client = ReactiveRippleClient(config)
client.init()

// Reactive event tracking
runBlocking {
    client.trackReactive("page_view", mapOf("page" to "home"))
}

// Event streaming with Kotlin Flow
client.getEventFlow()
    .collect { event ->
        println("Event tracked: ${event.name}")
    }

// Event streaming with Project Reactor
client.getEventFlux()
    .subscribe { event ->
        println("Event tracked: ${event.name}")
    }
```

## Testing

### Test Structure
- **Core Tests**: RippleClientTest, DispatcherTest, MetadataManagerTest
- **Mock Framework**: MockK for Kotlin-friendly mocking
- **Test Framework**: JUnit 5 with Kotlin coroutine testing support
- **Coverage**: Comprehensive unit tests for all public APIs

### Running Tests
```bash
# All tests
./gradlew test

# Specific module
./gradlew :core:test
./gradlew :android:test
./gradlew :spring:test
```

## Build System

### Multi-module Gradle Project
- **Version Catalog**: Centralized dependency management in `gradle/libs.versions.toml`
- **Convention Plugins**: Shared build logic in `buildSrc/`
- **Publishing**: GitHub Packages with Maven Central support (TODO)
- **CI/CD**: GitHub Actions for build, test, and release

### Key Dependencies
- **Kotlin**: 1.9.21 with coroutines 1.7.3
- **Serialization**: kotlinx-serialization-json 1.6.2
- **Android**: OkHttp 4.12.0, AppCompat 1.6.1
- **Spring**: Spring Boot 3.2.0, WebFlux
- **Reactive**: Project Reactor 3.6.0
- **Testing**: JUnit 5.10.1, MockK 1.13.8

## Performance Characteristics

### Memory Usage
- **Minimal Overhead**: Lightweight data structures, no unnecessary allocations
- **Event Pooling**: Events are simple data classes, GC-friendly
- **Queue Management**: Bounded by configuration, prevents memory leaks

### Threading Model
- **Main Thread**: All public APIs are non-blocking
- **Background Threads**: Dedicated thread pool for HTTP operations (2 threads)
- **Coroutines**: Structured concurrency for async operations
- **Synchronization**: Minimal locking, atomic operations where possible

### Network Efficiency
- **Batching**: Reduces HTTP requests by grouping events
- **Compression**: JSON payload compression (adapter-dependent)
- **Retry Strategy**: Intelligent backoff prevents server overload
- **Offline Support**: Graceful handling of network unavailability

## Error Handling

### Client Errors
- **IllegalStateException**: Client not initialized
- **Configuration Errors**: Invalid config parameters logged and handled

### Network Errors
- **HTTP Errors**: Automatic retry with exponential backoff
- **Timeout Errors**: Configurable timeouts in HTTP adapters
- **Connection Errors**: Events persisted for later retry

### Storage Errors
- **Persistence Failures**: Logged but don't block event tracking
- **Recovery Failures**: Graceful degradation, continue with empty queue

## Security Considerations

### API Key Management
- **Header-based**: Configurable API key header name
- **Environment Variables**: Support for secure credential storage
- **No Logging**: API keys never logged in debug output

### Data Privacy
- **No PII**: SDK doesn't collect personal information automatically
- **User Control**: All event data controlled by application
- **Metadata Filtering**: Applications can filter sensitive data

### Network Security
- **HTTPS**: Recommended for all API endpoints
- **Certificate Validation**: Standard SSL/TLS validation in HTTP adapters
- **Request Signing**: Can be implemented in custom HTTP adapters

## Monitoring and Observability

### Logging Levels
- **DEBUG**: Detailed operation logs, queue sizes, timing
- **INFO**: Lifecycle events, successful operations
- **WARN**: Retry attempts, configuration issues
- **ERROR**: Failed operations, critical errors

### Metrics (Available via Logging)
- **Queue Size**: Current number of queued events
- **Flush Success/Failure**: HTTP operation results
- **Retry Attempts**: Number of retry attempts per batch
- **Event Throughput**: Events processed per time period

### Health Checks
- **Initialization Status**: `isInitialized` flag
- **Queue Health**: Queue size monitoring
- **Network Health**: HTTP adapter success rates

## Migration and Compatibility

### Version Compatibility
- **Semantic Versioning**: Major.Minor.Patch versioning
- **Backward Compatibility**: Maintained within major versions
- **Deprecation Policy**: 2 minor versions notice for breaking changes

### Platform Requirements
- **Android**: API level 21+ (Android 5.0)
- **JVM**: Java 8+ / Kotlin 1.9+
- **Spring**: Spring Boot 3.0+ (Spring Framework 6.0+)

### Upgrade Path
- **Configuration Migration**: Automated config validation
- **Data Migration**: Storage format versioning
- **API Evolution**: Gradual deprecation of old APIs

## Troubleshooting

### Common Issues

1. **Events Not Sending**
   - Check network connectivity
   - Verify API key and endpoint
   - Check logs for HTTP errors
   - Ensure client is initialized

2. **High Memory Usage**
   - Check queue size with `getQueueSize()`
   - Reduce `flushInterval` or `maxBatchSize`
   - Ensure `dispose()` is called

3. **Performance Issues**
   - Monitor flush frequency
   - Check HTTP adapter timeouts
   - Verify background thread usage

### Debug Configuration
```kotlin
val config = RippleConfig(
    // ... other config
    adapters = AdapterConfig(
        // ... other adapters
        loggerAdapter = AndroidLogAdapter(LogLevel.DEBUG) // Enable debug logging
    )
)
```

### Support Resources
- **GitHub Issues**: Bug reports and feature requests
- **Documentation**: Comprehensive API documentation
- **Examples**: Sample implementations for all platforms
- **Community**: Kotlin/Android developer community support
