# Android Module

Android-specific Ripple SDK implementation with lifecycle awareness.

## Public API

### AndroidRippleClient
```kotlin
class AndroidRippleClient(context: Context, config: RippleConfig) : RippleClient(config) {
    // Inherits all RippleClient methods
    // Automatically manages Android session lifecycle
}
```

### Built-in Adapters
```kotlin
// HTTP
class OkHttpAdapter(client: OkHttpClient = OkHttpClient()) : HttpAdapter

// Storage  
class SharedPreferencesAdapter(context: Context, prefsName: String = "ripple_events") : StorageAdapter

// Logging
class AndroidLogAdapter(tag: String = "Ripple", logLevel: LogLevel = LogLevel.WARN) : LoggerAdapter
```

## Usage
```kotlin
val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(),
        storageAdapter = SharedPreferencesAdapter(context),
        loggerAdapter = AndroidLogAdapter()
    )
)

val client = AndroidRippleClient(context, config)
lifecycleScope.launch {
    client.init()
    client.track("user_login", mapOf("method" to "google"))
}
```

## Features
- Automatic session management tied to app lifecycle
- SharedPreferences-based event persistence
- OkHttp integration for reliable networking
- Android Log integration
