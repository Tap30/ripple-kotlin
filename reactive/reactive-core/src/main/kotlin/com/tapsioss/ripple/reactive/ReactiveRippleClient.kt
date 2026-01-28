package com.tapsioss.ripple.reactive

import com.tapsioss.ripple.core.DefaultRippleEvent
import com.tapsioss.ripple.core.DefaultRippleMetadata
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.RippleEvent
import com.tapsioss.ripple.core.RippleMetadata
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import reactor.core.publisher.Flux

/**
 * Reactive Ripple client with Flow and Flux support.
 * 
 * @param TEvents Event type implementing [RippleEvent] for type-safe tracking
 * @param TMetadata Metadata type implementing [RippleMetadata] for type-safe metadata
 */
class ReactiveRippleClient<TEvents : RippleEvent, TMetadata : RippleMetadata>(
    config: RippleConfig
) : RippleClient<TEvents, TMetadata>(config) {
    
    private val eventFlow = MutableSharedFlow<Event>()

    suspend fun trackReactive(event: TEvents) {
        track(event)
        emitEvent(event.name, event.toPayload())
    }

    suspend fun trackReactive(name: String, payload: Map<String, Any>? = null) {
        track(name, payload)
        emitEvent(name, payload)
    }

    private suspend fun emitEvent(name: String, payload: Map<String, Any>?) {
        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = getMetadata().ifEmpty { null },
            sessionId = getSessionId(),
            platform = getPlatform()
        )
        eventFlow.tryEmit(event)
    }

    fun getEventFlow(): Flow<Event> = eventFlow.asSharedFlow()

    fun getEventFlux(): Flux<Event> = Flux.create { sink -> sink.complete() }

    override fun getPlatform(): Platform = Platform.Server

    companion object {
        /**
         * Create an untyped Reactive client for simple usage.
         */
        fun create(config: RippleConfig): ReactiveRippleClient<DefaultRippleEvent, DefaultRippleMetadata> {
            return ReactiveRippleClient(config)
        }
    }
}
