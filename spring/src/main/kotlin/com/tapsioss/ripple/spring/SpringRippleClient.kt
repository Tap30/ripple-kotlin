package com.tapsioss.ripple.spring

import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import java.util.*

/**
 * Spring-specific Ripple client implementation
 */
class SpringRippleClient(
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionId = UUID.randomUUID().toString()

    override fun getSessionId(): String = sessionId

    override fun getPlatform(): Platform.Server {
        return Platform.Server(
            runtime = "Spring Boot ${getSpringBootVersion()}"
        )
    }

    private fun getSpringBootVersion(): String {
        return try {
            val clazz = Class.forName("org.springframework.boot.SpringBootVersion")
            val method = clazz.getMethod("getVersion")
            method.invoke(null) as? String ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
