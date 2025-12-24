# Ripple Spring WebFlux Adapter

Reactive HTTP adapter for Spring applications using WebClient.

## Installation

```kotlin
implementation("com.tapsioss.ripple:ripple-spring-webflux:1.0.0")
```

## Usage

```kotlin
import com.tapsioss.ripple.spring.webflux.WebClientAdapter

val config = RippleConfig(
    apiKey = "your-api-key",
    endpoint = "https://api.example.com/events",
    adapters = AdapterConfig(
        httpAdapter = WebClientAdapter(),
        storageAdapter = FileStorageAdapter(),
        loggerAdapter = Slf4jLoggerAdapter()
    )
)
```

## Features

- **Reactive HTTP client** with Spring WebFlux
- **Non-blocking I/O** operations
- **Automatic JSON serialization**
- **Connection pooling** and **HTTP/2 support**
- **Configurable timeouts** and **retry policies**

## Dependencies

This module automatically includes:
- `org.springframework.boot:spring-boot-starter-webflux`
- `com.fasterxml.jackson.module:jackson-module-kotlin`

No additional dependencies needed in your Spring Boot app.
