package cab.tapsi.ripple.reactive

import cab.tapsi.ripple.core.DefaultRippleEvent
import cab.tapsi.ripple.core.DefaultRippleMetadata
import cab.tapsi.ripple.core.Event
import cab.tapsi.ripple.core.Platform
import cab.tapsi.ripple.core.RippleClient
import cab.tapsi.ripple.core.RippleConfig
import cab.tapsi.ripple.core.RippleEvent
import cab.tapsi.ripple.core.RippleMetadata
import cab.tapsi.ripple.core.toJsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.JsonObject
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
        emitEvent(event.name, event.getPayload())
    }

    suspend fun trackReactive(name: String, payload: Map<String, Any>? = null) {
        track(name, payload)
        emitEvent(name, payload?.toJsonObject())
    }

    private suspend fun emitEvent(name: String, payload: JsonObject?) {
        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = getMetadata(),
            platform = getPlatform(),
            anonymousId = getAnonymousId(),
            userId = getUserId()
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
