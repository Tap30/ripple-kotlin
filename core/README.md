# Core Module

Shared core logic and interfaces for Ripple SDK.

## Public API

### RippleClient (Abstract)
```kotlin
abstract class RippleClient(config: RippleConfig) {
    suspend fun init()
    suspend fun track(name: String, payload: Map<String, Any>? = null, metadata: Map<String, Any>? = null)
    fun setMetadata(key: String, value: Any)
    suspend fun flush()
    fun dispose()
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
    suspend fun send(endpoint: String, events: List<Event>, headers: Map<String, String>, apiKeyHeader: String): HttpResponse
}

interface StorageAdapter {
    suspend fun save(events: List<Event>)
    suspend fun load(): List<Event>
    suspend fun clear()
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
- Coroutine-based concurrency safety
