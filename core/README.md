# Core Module

Shared core logic and interfaces for Ripple SDK.

## Public API

### RippleClient (Abstract)
```kotlin
abstract class RippleClient(config: RippleConfig) {
    fun init()
    fun track(name: String, payload: Map<String, Any>? = null, schemaVersion: String? = null)
    fun <T : RippleEvent> track(event: T)
    fun setMetadata(key: String, value: Any)
    fun getMetadata(): Map<String, Any>?
    fun flush()
    fun dispose()
}
```

Type-safe events expose JSON payloads directly:

```kotlin
interface RippleEvent {
    val name: String
    val schemaVersion: String?
        get() = null

    fun getPayload(): JsonObject?
}
```

### Configuration
```kotlin
data class RippleConfig(
    val apiKey: String,
    val endpoint: String,
    val apiKeyHeader: String = "X-API-Key",
    val flushInterval: Long = 5000L,
    val maxBatchSize: Int = 10,
    val maxRetries: Int = 3,
    val adapters: AdapterConfig
)
```

### Adapters
```kotlin
interface HttpAdapter {
    fun send(endpoint: String, events: List<Event>, headers: Map<String, String>, apiKeyHeader: String): HttpResponse
}

interface StorageAdapter {
    fun save(events: List<Event>)
    fun load(): List<Event>
    fun clear()
}

interface LoggerAdapter {
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}
```

## Features
- Type-safe metadata management
- Automatic batching and retry logic
- Pluggable adapters for HTTP, storage, and logging
- Thread-safe queueing and background dispatch
