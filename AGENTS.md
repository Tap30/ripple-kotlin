# Ripple Kotlin SDK - Technical Documentation

A high-performance, scalable, and fault-tolerant event tracking SDK for Kotlin and Java applications with multi-platform support.

## Architecture Overview

The SDK follows a modular architecture with maximum code sharing and platform-specific optimizations:

```
ripple-kotlin/
├── core/                    # Shared core logic (thread-safe, coroutine-free)
│   ├── RippleClient         # Abstract client with lifecycle management
│   ├── RippleEvent          # Interface for type-safe events
│   ├── RippleMetadata       # Interface for type-safe metadata
│   ├── Dispatcher           # Queue management, batching, retry logic
│   ├── MetadataManager      # Thread-safe metadata handling
│   ├── Platform             # Sealed class for platform types
│   └── adapters/            # Pluggable adapter interfaces
├── android/                 # Android-specific implementation
│   ├── android-core/        # AndroidRippleClient
│   └── adapters/            # OkHttp, Room, SharedPreferences, Android Log
├── spring/                  # Spring Boot integration
│   ├── spring-core/         # SpringRippleClient
│   └── adapters/            # WebClient, File storage, SLF4J
├── reactive/                # Reactive streams support
│   ├── reactive-core/       # ReactiveRippleClient
│   └── adapters/            # Reactor support
└── samples/                 # Example implementations
    ├── android-sample/      # Android app with E2E tests
    ├── spring-sample/       # Spring Boot example
    ├── spring-java-sample/  # Java Spring Boot example
    └── test-server/         # Local Ktor server for E2E testing
```

## Core Features

### 🚀 High Performance
- **O(1) Queue Operations**: ConcurrentLinkedQueue for thread-safe event queuing
- **Automatic Batching**: Configurable batch size (default: 10 events)
- **Non-blocking API**: All public methods return immediately
- **Background Processing**: Dedicated thread pool for HTTP operations

### 🔄 Fault Tolerance
- **Exponential Backoff**: Retry with jitter (0-1000ms), max 30s delay
- **Smart Retry Logic**: 5xx errors retry, 4xx errors don't retry
- **Offline Persistence**: Events saved to storage when network fails
- **FIFO Order Maintained**: Failed events prepended to queue

### 🔒 Thread Safety
- **Mutex-protected Flushes**: Only one flush operation at a time
- **Concurrent Collections**: ConcurrentLinkedQueue, ConcurrentHashMap
- **Atomic Operations**: AtomicBoolean for state management
- **Synchronized Initialization**: Double-checked locking pattern

### 📦 Pluggable Architecture
- **HTTP Adapters**: OkHttp (Android), WebClient (Spring), custom
- **Storage Adapters**: SharedPreferences, Room, File system, custom
- **Logger Adapters**: Android Log, SLF4J, Console, NoOp

## Type-Safe API

### RippleEvent Interface

Define type-safe events with compile-time validation:

```kotlin
interface RippleEvent {
    val name: String
    fun toPayload(): Map<String, Any>?
}

// User implementation
sealed class AppEvent : RippleEvent {
    data class UserLogin(val email: String, val method: String) : AppEvent() {
        override val name = "user.login"
        override fun toPayload() = mapOf("email" to email, "method" to method)
    }
    
    data class Purchase(val orderId: String, val amount: Double) : AppEvent() {
        override val name = "purchase"
        override fun toPayload() = mapOf("orderId" to orderId, "amount" to amount)
    }
}

// Type-safe tracking
client.track(AppEvent.UserLogin("user@example.com", "google"))
```

### RippleMetadata Interface

Define type-safe metadata:

```kotlin
interface RippleMetadata {
    fun toMap(): Map<String, Any>
}

// User implementation
data class AppMetadata(
    val userId: String? = null,
    val version: String? = null
) : RippleMetadata {
    override fun toMap() = buildMap {
        userId?.let { put("userId", it) }
        version?.let { put("version", it) }
    }
}

// Type-safe metadata
client.setMetadata(AppMetadata(userId = "123", version = "1.0.0"))
client.track(event, AppMetadata(userId = "vip-user"))
```

### Track Method Overloads

```kotlin
// Type-safe event
fun <T : RippleEvent> track(event: T, metadata: RippleMetadata? = null)
fun <T : RippleEvent> track(event: T, metadata: Map<String, Any>?)

// Untyped event
fun track(name: String, payload: Map<String, Any>? = null, metadata: Map<String, Any>? = null)
fun track(name: String, payload: Map<String, Any>?, metadata: RippleMetadata)
```

## Core Module

### RippleClient (Abstract Base)

```kotlin
abstract class RippleClient(protected val config: RippleConfig) {
    // Lifecycle
    fun init()                                    // Initialize, restore events, start scheduler
    fun dispose()                                 // Clean shutdown, persist events, supports re-init
    
    // Type-safe event tracking
    fun <T : RippleEvent> track(event: T, metadata: RippleMetadata? = null)
    fun <T : RippleEvent> track(event: T, metadata: Map<String, Any>?)
    
    // Untyped event tracking
    fun track(name: String, payload: Map<String, Any>?, metadata: Map<String, Any>?)
    fun track(name: String, payload: Map<String, Any>?, metadata: RippleMetadata)
    
    // Metadata management
    fun setMetadata(metadata: RippleMetadata)     // Type-safe
    fun setMetadata(key: String, value: Any)      // Untyped
    fun getMetadata(): Map<String, Any>
    fun removeMetadata(key: String)
    fun clearMetadata()
    
    // Session
    fun getSessionId(): String?                   // Format: {timestamp}-{random}
    
    // Flushing
    fun flush()                                   // Non-blocking
    fun flushSync()                              // Blocking
    fun getQueueSize(): Int
    
    // Platform-specific
    protected abstract fun getPlatform(): Platform?
}
```

**Key Implementation Details:**
- Dispatcher is recreated on each `init()` call (supports re-initialization)
- Session ID generated on `init()`, cleared on `dispose()`
- Default logger is `ConsoleLoggerAdapter` with WARN level
- Session ID format: `{timestamp}-{random}` (e.g., `1704567890123-456789`)

### Dispatcher (Queue Management)

**Retry Logic:**
- 2xx: Success, clear storage
- 4xx: No retry, persist events immediately
- 5xx: Retry with exponential backoff
- Network error: Retry with exponential backoff

**Backoff Formula:**
```
delay = (1000ms × 2^attempt) + jitter(0-1000ms)
max delay = 30 seconds
```

**FIFO Ordering:**
Failed events are prepended to queue, not appended.

### Platform (Sealed Class)

```kotlin
sealed class Platform {
    abstract val type: String
    
    data class Web(
        val browser: DeviceInfo,
        val device: DeviceInfo,
        val os: DeviceInfo
    ) : Platform()  // type = "web"
    
    data class Native(
        val device: DeviceInfo,
        val os: DeviceInfo
    ) : Platform()  // type = "native"
    
    data object Server : Platform()  // type = "server"
}

data class DeviceInfo(val name: String, val version: String)
```

### Built-in Loggers

```kotlin
class ConsoleLoggerAdapter(level: LogLevel = LogLevel.WARN) : LoggerAdapter
class NoOpLoggerAdapter : LoggerAdapter
```

## Platform Modules

### Android Module

```kotlin
class AndroidRippleClient(config: RippleConfig) : RippleClient(config) {
    override fun getPlatform(): Platform.Native
}
```

Platform detection:
- `device.name`: `Build.MODEL`
- `device.version`: `Build.MANUFACTURER`
- `os.name`: "Android"
- `os.version`: `Build.VERSION.RELEASE`

### Spring Module

```kotlin
class SpringRippleClient(config: RippleConfig) : RippleClient(config) {
    override fun getPlatform(): Platform.Server
}
```

### Reactive Module

```kotlin
class ReactiveRippleClient(config: RippleConfig) : RippleClient(config) {
    suspend fun <T : RippleEvent> trackReactive(event: T)
    suspend fun trackReactive(name: String, payload: Map<String, Any>?)
    fun getEventFlow(): Flow<Event>
    fun getEventFlux(): Flux<Event>
}
```

## Configuration

### RippleConfig
```kotlin
data class RippleConfig(
    val apiKey: String,                         // Required
    val endpoint: String,                       // Required
    val apiKeyHeader: String = "X-API-Key",
    val flushInterval: Long = 5000L,            // ms
    val maxBatchSize: Int = 10,
    val maxRetries: Int = 3,
    val adapters: AdapterConfig
)
```

### AdapterConfig
```kotlin
data class AdapterConfig(
    val httpAdapter: HttpAdapter,               // Required
    val storageAdapter: StorageAdapter,         // Required
    val loggerAdapter: LoggerAdapter? = null    // Defaults to ConsoleLoggerAdapter(WARN)
)
```

## Testing

### Test Structure
- **Unit Tests**: RippleClientTest, DispatcherTest, MetadataManagerTest
- **Concurrency Tests**: ConcurrencyTest (thread safety verification)
- **E2E Tests**: Android instrumented tests with local test server

### Running Tests
```bash
./gradlew :core:test                    # Core unit tests
./gradlew :samples:test-server:run      # Start E2E test server
./gradlew :samples:android-sample:connectedDebugAndroidTest  # E2E tests
```

### Concurrency Tests Cover
- Concurrent `track()` calls
- Concurrent `setMetadata()` calls
- Concurrent `flush()` calls (mutex verification)
- `track()` and `dispose()` race conditions
- Concurrent `init()` calls (idempotency)

## Build System

### Module Structure
```
:core                           # Shared core
:android:android-core           # Android client
:android:adapters:okhttp        # OkHttp adapter
:android:adapters:room          # Room storage adapter
:android:adapters:logging       # Android Log adapter
:android:adapters:storage-preferences  # SharedPreferences adapter
:spring:spring-core             # Spring client
:spring:adapters:webflux        # WebClient adapter
:spring:adapters:logging        # SLF4J adapter
:spring:adapters:storage-file   # File storage adapter
:reactive:reactive-core         # Reactive client
:reactive:adapters:reactor      # Reactor adapter
:samples:android-sample         # Android sample app
:samples:spring-sample          # Spring sample
:samples:test-server            # E2E test server
```

### Publishing
- GitHub Packages: Automated on git tag push
- Maven Central: Configured, requires secrets setup
- JAR naming: `archiveBaseName` matches `artifactId`

## API Contract Compliance

This implementation follows the [Ripple SDK API Contract](https://github.com/Tap30/ripple/blob/main/DESIGN_AND_CONTRACTS.md):

| Contract Requirement | Status | Notes |
|---------------------|--------|-------|
| `init()` before `track()` | ✅ | Throws IllegalStateException |
| Re-initialization after dispose | ✅ | Dispatcher recreated |
| `getSessionId()` public | ✅ | Returns null before init |
| `getMetadata()` method | ✅ | Returns shallow copy |
| Session ID format | ✅ | `{timestamp}-{random}` |
| Platform discriminated union | ✅ | Sealed class with Web/Native/Server |
| 4xx no retry | ✅ | Immediate persist |
| 5xx retry | ✅ | Exponential backoff |
| FIFO order on requeue | ✅ | Failed events prepended |
| Jitter 0-1000ms | ✅ | Random.nextLong(0, 1000) |
| Default logger | ✅ | ConsoleLoggerAdapter(WARN) |
| Concurrency tests | ✅ | ConcurrencyTest.kt |
| Multi-instance support | ✅ | Each instance independent |
| Type-safe events | ✅ | RippleEvent interface |
| Type-safe metadata | ✅ | RippleMetadata interface |

## Changelog

### v1.0.0-alpha.4 (2026-01-07)
- **Added**: `RippleEvent` interface for type-safe event tracking
- **Added**: `RippleMetadata` interface for type-safe metadata
- **Added**: Multiple `track()` overloads for typed/untyped usage
- **Changed**: Session ID now managed internally by base client
- **Changed**: `dispose()` clears metadata and session ID
- **Removed**: Generic type parameters from client classes

### v1.0.0-alpha.3 (2026-01-07)
- **Breaking**: Platform changed from data class to sealed class
- **Breaking**: `getSessionId()` now public (was protected)
- **Added**: `getMetadata()` method
- **Added**: Re-initialization support after `dispose()`
- **Added**: `ConsoleLoggerAdapter` and `NoOpLoggerAdapter`
- **Added**: Concurrency tests
- **Fixed**: 4xx errors no longer retry
- **Fixed**: Failed events maintain FIFO order on requeue
- **Fixed**: Jitter range corrected to 0-1000ms
- **Fixed**: Session ID format changed to `{timestamp}-{random}`

### v1.0.0-alpha.2
- Modular adapter architecture
- Room storage adapter
- GitHub Actions CI/CD
- Maven Central publishing setup

### v1.0.0-alpha.1
- Initial release
- Core SDK with Android and Spring support
