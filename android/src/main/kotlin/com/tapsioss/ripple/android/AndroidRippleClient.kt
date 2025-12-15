package com.tapsioss.ripple.android

import android.content.Context
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig

/**
 * Android-specific Ripple client with automatic platform detection and session management.
 * 
 * @param context Android context for platform information
 * @param config Client configuration with adapters
 */
class AndroidRippleClient(
    private val context: Context,
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionManager = SessionManager()
    private val platformDetector = PlatformDetector()

    /**
     * Initialize client and start session tracking.
     * Automatically detects Android device and OS information.
     */
    override suspend fun init() {
        sessionManager.initSession()
        super.init()
    }

    /**
     * Get current session ID for event tracking.
     * 
     * @return Session ID string or null if not initialized
     */
    override fun getSessionId(): String? = sessionManager.getSessionId()

    /**
     * Get Android platform information including device and OS details.
     * 
     * @return Platform object with Android-specific information
     */
    override fun getPlatform(): Platform = platformDetector.getPlatform()
}
