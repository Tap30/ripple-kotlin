package com.tapsioss.ripple.reactive

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.SessionIdGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import reactor.core.publisher.Flux

/**
 * Reactive Ripple client with Flow and Flux support.
 */
class ReactiveRippleClient(
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionId = SessionIdGenerator.generate()
    private val eventFlow = MutableSharedFlow<Event>()

    suspend fun trackReactive(
        name: String,
        payload: Map<String, Any>? = null,
        metadata: Map<String, Any>? = null
    ) {
        track(name, payload, metadata)
        
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

    fun getEventFlow(): Flow<Event> = eventFlow.asSharedFlow()

    fun getEventFlux(): Flux<Event> = Flux.create { sink -> sink.complete() }

    override fun getSessionId(): String = sessionId

    override fun getPlatform(): Platform = Platform.Server
}
