# Ripple Kotlin SDK - Technical Documentation

A high-performance, scalable, and fault-tolerant event tracking SDK for Kotlin and Java applications with multi-platform support.

## Architecture Overview

The SDK follows a modular architecture with maximum code sharing and platform-specific optimizations:

```
ripple-kotlin/
├── core/                    # Shared core logic (thread-safe, coroutine-free)
│   ├── RippleClient         # Abstract client with lifecycle management
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

## Core Module

### RippleClient (Abstract Base)

```kotlin
abstract class RippleClient(protected val config: RippleConfig) {
    // Lifecycle
    fun init()                                    // Initialize, restore events, start scheduler
    fun dispose()                                 // Clean shutdown, persist events, supports re-init
    
    // Event tracking
    fun track(name: String, payload: Map<String, Any>?, metadata: Map<String, Any>?)
    
    // Metadata management
    fun setMetadata(key: String, value: Any)
    fun getMetadata(): Map<String, Any>           // Returns shallow copy
    fun removeMetadata(key: String)
    fun clearMetadata()
    
    // Session
    abstract fun getSessionId(): String?          // Public - format: {timestamp}-{random}
    
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
- Default logger is `ConsoleLoggerAdapter` with WARN level
- Session ID format: `{timestamp}-{random}` (e.g., `1704567890123-456789`)

### Dispatcher (Queue Management)

```kotlin
class Dispatcher(
    config: DispatcherConfig,
    httpAdapter: HttpAdapter,
    storageAdapter: StorageAdapter,
    loggerAdapter: LoggerAdapter  // Non-nullable, defaults to ConsoleLoggerAdapter
)
```

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

Per API contract, Platform is a discriminated union:

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
// Default logger with configurable level
class ConsoleLoggerAdapter(level: LogLevel = LogLevel.WARN) : LoggerAdapter

// Silent logger
class NoOpLoggerAdapter : LoggerAdapter
```

## Platform Modules

### Android Module

```kotlin
class AndroidRippleClient(context: Context, config: RippleConfig) : RippleClient(config) {
    override fun getSessionId(): String  // Format: {timestamp}-{random}
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
    override fun getSessionId(): String  // Format: {timestamp}-{random}
    override fun getPlatform(): Platform.Server
}
```

### Reactive Module

```kotlin
class ReactiveRippleClient(config: RippleConfig) : RippleClient(config) {
    suspend fun trackReactive(name: String, payload: Map<String, Any>?, metadata: Map<String, Any>?)
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
| `getSessionId()` public | ✅ | Abstract in base, implemented in platforms |
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

## Changelog

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
