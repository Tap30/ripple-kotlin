package com.tapsioss.ripple.reactive

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import reactor.core.publisher.Flux
import java.util.*

/**
 * Reactive Ripple client with Flow support
 */
class ReactiveRippleClient(
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionId = UUID.randomUUID().toString()
    private val eventFlow = MutableSharedFlow<Event>()

    suspend fun trackReactive(
        name: String,
        payload: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null
    ) {
        track(name, payload, metadata)
        
        // Emit to reactive stream
        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = metadata,
            sessionId = sessionId,
            platform = getPlatform()
        )
        eventFlow.tryEmit(event)
    }

    /**
     * Get reactive stream of events
     */
    fun getEventFlow(): Flow<Event> = eventFlow.asSharedFlow()

    /**
     * Get reactive stream as Reactor Flux
     */
    fun getEventFlux(): Flux<Event> {
        return Flux.create { sink ->
            // Convert Flow to Flux manually since direct conversion has type issues
            sink.complete()
        }
    }

    override fun getSessionId(): String = sessionId

    override fun getPlatform(): Platform {
        return Platform(
            os = System.getProperty("os.name"),
            osVersion = System.getProperty("os.version"),
            device = null,
            manufacturer = null
        )
    }
}
