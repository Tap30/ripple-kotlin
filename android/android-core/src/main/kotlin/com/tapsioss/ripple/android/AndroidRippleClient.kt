package com.tapsioss.ripple.android

import android.content.Context
import com.tapsioss.ripple.core.DeviceInfo
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.SessionIdGenerator

/**
 * Android-specific Ripple client implementation.
 * 
 * Provides event tracking optimized for Android applications with
 * automatic session management and platform detection.
 * 
 * @param context Android application context
 * @param config Ripple configuration
 */
class AndroidRippleClient(
    private val context: Context,
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionId: String = SessionIdGenerator.generate()

    override fun getSessionId(): String = sessionId

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
}
