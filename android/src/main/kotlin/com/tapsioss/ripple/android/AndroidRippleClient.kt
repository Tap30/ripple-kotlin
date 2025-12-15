package com.tapsioss.ripple.android

import android.content.Context
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig

/**
 * Android-specific Ripple client implementation
 */
class AndroidRippleClient(
    private val context: Context,
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionManager = SessionManager()
    private val platformDetector = PlatformDetector()

    override suspend fun init() {
        sessionManager.initSession()
        super.init()
    }

    override fun getSessionId(): String? = sessionManager.getSessionId()

    override fun getPlatform(): Platform = platformDetector.getPlatform()
}
