package com.tapsioss.ripple.reactive

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.reactive.asFlow
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

    override suspend fun track(
        name: String,
        payload: Map<String, Any>?,
        metadata: Map<String, Any>?
    ) {
        super.track(name, payload, metadata)
        
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
    fun getEventFlux(): Flux<Event> = Flux.from(eventFlow.asSharedFlow().asFlow())

    override fun getSessionId(): String = sessionId

    override fun getPlatform(): Platform.Server {
        return Platform.Server(runtime = "Reactive")
    }
}
