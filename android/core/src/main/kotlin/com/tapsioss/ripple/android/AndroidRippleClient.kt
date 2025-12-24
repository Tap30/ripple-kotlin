package com.tapsioss.ripple.android

import android.content.Context
import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import java.util.*

/**
 * Android-specific Ripple client implementation.
 * 
 * Provides event tracking optimized for Android applications with
 * automatic session management and platform detection.
 * 
 * Example usage:
 * ```kotlin
 * val config = RippleConfig(
 *     apiKey = "your-api-key",
 *     endpoint = "https://api.example.com/events",
 *     adapters = AdapterConfig(
 *         httpAdapter = OkHttpAdapter(),
 *         storageAdapter = SharedPreferencesAdapter(context),
 *         loggerAdapter = AndroidLogAdapter()
 *     )
 * )
 * 
 * val client = AndroidRippleClient(context, config)
 * client.init()
 * client.track("app_opened")
 * ```
 * 
 * @param context Android application context
 * @param config Ripple configuration
 */
class AndroidRippleClient(
    private val context: Context,
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionManager = SessionManager()
    private val platformDetector = PlatformDetector()

    override fun getSessionId(): String? = sessionManager.getSessionId()

    override fun getPlatform(): Platform? = platformDetector.getPlatform()
}

/**
 * Manages session IDs for event tracking.
 */
internal class SessionManager {
    private val sessionId: String = UUID.randomUUID().toString()
    
    fun getSessionId(): String = sessionId
}

/**
 * Detects platform information for event context.
 */
internal class PlatformDetector {
    fun getPlatform(): Platform = Platform(
        os = "Android",
        osVersion = android.os.Build.VERSION.RELEASE,
        device = android.os.Build.MODEL,
        manufacturer = android.os.Build.MANUFACTURER
    )
}
