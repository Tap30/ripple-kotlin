package com.tapsioss.ripple.spring

import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.SessionIdGenerator

/**
 * Spring-specific Ripple client implementation.
 * 
 * Provides event tracking optimized for Spring Boot applications.
 * Thread-safe and suitable for use as a singleton bean.
 * 
 * @param config Ripple configuration
 */
class SpringRippleClient(
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionId: String = SessionIdGenerator.generate()

    override fun getSessionId(): String = sessionId

    override fun getPlatform(): Platform = Platform.Server
}
