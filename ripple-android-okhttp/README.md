# Ripple Android OkHttp Adapter

High-performance HTTP adapter for Android using OkHttp.

## Installation

```kotlin
implementation("com.tapsioss.ripple:ripple-android-okhttp:1.0.0")
```

## Usage

```kotlin
import com.tapsioss.ripple.android.okhttp.OkHttpAdapter

val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(),
        storageAdapter = SharedPreferencesAdapter(context),
        loggerAdapter = AndroidLogAdapter()
    )
)
```

## Features

- **Reliable networking** with OkHttp
- **Configurable timeouts** (30s default)
- **Automatic JSON serialization** 
- **Connection pooling** and **HTTP/2 support**
- **Retry and redirect handling**

## Dependencies

This module automatically includes:
- `com.squareup.okhttp3:okhttp:4.12.0`
- `org.jetbrains.kotlinx:kotlinx-serialization-json`

No additional dependencies needed in your app.
