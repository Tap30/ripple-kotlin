# Reactive Module

Reactive streams support for Ripple SDK with Kotlin Flow and Project Reactor.

## Public API

### ReactiveRippleClient
```kotlin
class ReactiveRippleClient(config: RippleConfig) : RippleClient(config) {
    // Inherits all RippleClient methods
    
    suspend fun trackReactive(name: String, payload: Map<String, Any>? = null, metadata: Map<String, Any>? = null)
    fun getEventFlow(): Flow<Event>
    fun getEventFlux(): Flux<Event>
}
```

## Usage

### With Kotlin Flow
```kotlin
val client = ReactiveRippleClient(config)
client.init()

// Track events and observe stream
client.trackReactive("user_action", mapOf("action" to "click"))

client.getEventFlow()
    .collect { event ->
        println("Event: ${event.name}")
    }
```

### With Project Reactor
```kotlin
client.getEventFlux()
    .subscribe { event ->
        println("Event: ${event.name}")
    }
```

## Features
- Kotlin Flow integration for reactive event streams
- Project Reactor Flux support
- Real-time event observation
- Backpressure handling
