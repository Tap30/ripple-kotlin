# Ripple Kotlin SDK

A high-performance event tracking SDK for Kotlin and Java applications.

## Installation

```kotlin
// Android
implementation("com.tapsioss.ripple:android:1.0.0")

// Spring Boot
implementation("com.tapsioss.ripple:spring:1.0.0")

// Reactive
implementation("com.tapsioss.ripple:reactive:1.0.0")
```

## Quick Start

```kotlin
val client = AndroidRippleClient(context, config)

lifecycleScope.launch {
    client.track("user_login", mapOf("method" to "google"))
}
```

## Features

- 🚀 High performance with automatic batching
- 🔄 Built-in retry logic and error handling
- 🔒 Thread-safe coroutine-based operations
- 🌐 Multi-platform support (Android, Spring, Reactive)
- 📘 Full Kotlin and Java compatibility

## Documentation

- [Detailed Documentation](AGENTS.md) - Complete API reference and examples
- [Publishing Guide](PUBLISHING.md) - How to publish to Maven Central
- [Contributing](CONTRIBUTING.md) - Development guidelines

## License

MIT License - see [LICENSE](LICENSE) file for details.
