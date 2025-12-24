# Ripple Android Room Adapter

High-performance storage adapter for Android using Room database.

## Installation

```kotlin
implementation("com.tapsioss.ripple:ripple-android-room:1.0.0")
```

## Usage

```kotlin
import com.tapsioss.ripple.android.room.RoomStorageAdapterFactory

val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = OkHttpAdapter(),
        storageAdapter = RoomStorageAdapterFactory.create(context),
        loggerAdapter = AndroidLogAdapter()
    )
)
```

## Performance Benefits

- **~10x faster** than SharedPreferences for large datasets
- **Async operations** - non-blocking I/O
- **Batch inserts** for multiple events
- **Structured queries** with type safety
- **Better memory efficiency**

## Features

- **SQLite database** with Room ORM
- **Coroutine support** for async operations
- **Automatic migrations** (destructive in v1)
- **JSON serialization** for complex event data
- **Factory pattern** for easy setup

## Dependencies

This module automatically includes:
- `androidx.room:room-runtime:2.6.1`
- `androidx.room:room-ktx:2.6.1`

No additional dependencies or annotation processors needed in your app.
