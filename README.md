# Ripple Kotlin SDK

A high-performance event tracking SDK for Kotlin and Java applications.

## Download

### GitHub Packages (Current)

Add to your `build.gradle.kts`:

```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Tap30/ripple-kotlin")
        credentials {
            username = "your_github_username"
            password = "your_github_token"
        }
    }
}

dependencies {
    // Android
    implementation("com.tapsioss.ripple:android:1.0.0")
    
    // Spring Boot
    implementation("com.tapsioss.ripple:spring:1.0.0")
    
    // Reactive
    implementation("com.tapsioss.ripple:reactive:1.0.0")
    
    // Core (for custom implementations)
    implementation("com.tapsioss.ripple:core:1.0.0")
}
```

### Maven Central (Coming Soon)

```kotlin
// Will be available without authentication
implementation("com.tapsioss.ripple:android:1.0.0")
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
- [Changelog](CHANGELOG.md) - Release history

## License

MIT License - see [LICENSE](LICENSE) file for details.
