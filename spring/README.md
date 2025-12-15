# Spring Module

Spring Boot integration for Ripple SDK with reactive support.

## Public API

### SpringRippleClient
```kotlin
class SpringRippleClient(config: RippleConfig) : RippleClient(config) {
    // Inherits all RippleClient methods
    // Optimized for Spring Boot environments
}
```

### Built-in Adapters
```kotlin
// HTTP
class WebClientAdapter(webClient: WebClient = WebClient.create()) : HttpAdapter

// Storage
class FileStorageAdapter(filePath: String = "./ripple_events.json") : StorageAdapter

// Logging
class Slf4jLoggerAdapter(logger: Logger = LoggerFactory.getLogger("Ripple")) : LoggerAdapter
```

## Usage
```kotlin
@Bean
fun rippleClient(): SpringRippleClient {
    val config = RippleConfig(
        apiKey = "your-api-key",
        endpoint = "https://api.example.com/events",
        adapters = AdapterConfig(
            httpAdapter = WebClientAdapter(),
            storageAdapter = FileStorageAdapter(),
            loggerAdapter = Slf4jLoggerAdapter()
        )
    )
    return SpringRippleClient(config)
}

@Service
class UserService(private val rippleClient: SpringRippleClient) {
    suspend fun createUser(user: User) {
        rippleClient.track("user_created", mapOf("userId" to user.id))
    }
}
```

## Features
- WebClient-based reactive HTTP calls
- File system event persistence
- SLF4J logging integration
- Spring Boot auto-configuration ready
