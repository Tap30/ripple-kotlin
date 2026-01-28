package com.tapsioss.ripple.spring

import com.tapsioss.ripple.core.DefaultRippleEvent
import com.tapsioss.ripple.core.DefaultRippleMetadata
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.RippleEvent
import com.tapsioss.ripple.core.RippleMetadata

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
