package com.tapsioss.ripple.android

import com.tapsioss.ripple.core.DefaultRippleEvent
import com.tapsioss.ripple.core.DefaultRippleMetadata
import com.tapsioss.ripple.core.DeviceInfo
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.RippleEvent
import com.tapsioss.ripple.core.RippleMetadata

/**
 * Android-specific Ripple client.
 * 
 * @param TEvents Event type implementing [RippleEvent] for type-safe tracking
 * @param TMetadata Metadata type implementing [RippleMetadata] for type-safe metadata
 * @param config Ripple configuration
 */
class AndroidRippleClient<TEvents : RippleEvent, TMetadata : RippleMetadata>(
    config: RippleConfig
) : RippleClient<TEvents, TMetadata>(config) {

    override fun getPlatform(): Platform = Platform.Native(
        device = DeviceInfo(
            name = android.os.Build.MODEL,
            version = android.os.Build.MANUFACTURER
        ),
        os = DeviceInfo(
            name = "Android",
            version = android.os.Build.VERSION.RELEASE
        )
    )

    companion object {
        /**
         * Create an untyped Android client for simple usage.
         */
        fun create(config: RippleConfig): AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata> {
            return AndroidRippleClient(config)
        }
    }
}
