# Android Module

Pure Kotlin JVM library for Android integration with Ripple SDK.

## Public API

### AndroidRippleClient
```kotlin
class AndroidRippleClient(context: Context, config: RippleConfig) : RippleClient(config) {
    // Inherits all RippleClient methods
    // Uses Android Context for platform-specific features
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
Add to your Android app's `build.gradle.kts`:
```kotlin
implementation("com.tapsioss.ripple:android:1.0.0")
```

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
- Pure Kotlin JVM library (no Android AGP required)
- SharedPreferences-based event persistence
- Android platform detection (device, OS info)
- OkHttp integration for reliable networking
- Lightweight with minimal dependencies

## Benefits of Pure Kotlin Approach
- No Android Gradle Plugin dependency
- Faster compilation
- Can be used in any Kotlin/Java project
- Smaller artifact size
- No Android manifest required
