# Android Module

Pure Kotlin JVM library for Android integration with Ripple SDK.

## Installation

```kotlin
// Core Android module (lightweight)
implementation("com.tapsioss.ripple:android:1.0.0")

// Optional adapters (choose what you need)
implementation("com.tapsioss.ripple:ripple-android-okhttp:1.0.0")    // HTTP with OkHttp
implementation("com.tapsioss.ripple:ripple-android-room:1.0.0")      // Storage with Room
```

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
// Storage (included in core android module)
class SharedPreferencesAdapter(context: Context, prefsName: String = "ripple_events") : StorageAdapter

// Logging (included in core android module)
class AndroidLogAdapter(tag: String = "Ripple", logLevel: LogLevel = LogLevel.WARN) : LoggerAdapter
```

### Optional Adapters (Separate Modules)

#### HTTP Adapters
- **OkHttpAdapter** (`ripple-android-okhttp`) - High-performance HTTP with OkHttp

#### Storage Adapters  
- **RoomStorageAdapter** (`ripple-android-room`) - SQLite database with Room ORM

## Usage Examples

### Basic Setup (SharedPreferences)
```kotlin
val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(), // Requires ripple-android-okhttp
        storageAdapter = SharedPreferencesAdapter(context), // Built-in
        loggerAdapter = AndroidLogAdapter() // Built-in
    )
)
```

### High-Performance Setup (Room Database)
```kotlin
val config = RippleConfig(
    apiKey = "your-api-key", 
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(), // Requires ripple-android-okhttp
        storageAdapter = RoomStorageAdapterFactory.create(context), // Requires ripple-android-room
        loggerAdapter = AndroidLogAdapter()
    )
)
```

## Features
- Pure Kotlin JVM library (no Android AGP required)
- SharedPreferences-based event persistence (built-in)
- Android platform detection (device, OS info)
- Lightweight with minimal dependencies
- Optional high-performance adapters

## Benefits of Modular Approach
- **Smaller app size** - only include adapters you need
- **No forced dependencies** - OkHttp and Room are optional
- **Faster compilation** - fewer transitive dependencies
- **Flexible architecture** - mix and match adapters
