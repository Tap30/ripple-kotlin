package cab.tapsi.ripple.spring

import cab.tapsi.ripple.core.DefaultRippleEvent
import cab.tapsi.ripple.core.DefaultRippleMetadata
import cab.tapsi.ripple.core.Platform
import cab.tapsi.ripple.core.RippleClient
import cab.tapsi.ripple.core.RippleConfig
import cab.tapsi.ripple.core.RippleEvent
import cab.tapsi.ripple.core.RippleMetadata

/**
 * Spring-specific Ripple client.
 * 
 * @param TEvents Event type implementing [RippleEvent] for type-safe tracking
 * @param TMetadata Metadata type implementing [RippleMetadata] for type-safe metadata
 * @param config Ripple configuration
 */
class SpringRippleClient<TEvents : RippleEvent, TMetadata : RippleMetadata>(
    config: RippleConfig
) : RippleClient<TEvents, TMetadata>(config) {

    override fun getPlatform(): Platform = Platform.Server

    companion object {
        /**
         * Create an untyped Spring client for simple usage.
         */
        fun create(config: RippleConfig): SpringRippleClient<DefaultRippleEvent, DefaultRippleMetadata> {
            return SpringRippleClient(config)
        }
    }
}
