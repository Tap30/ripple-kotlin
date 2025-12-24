package com.tapsioss.ripple.spring

import com.tapsioss.ripple.core.Platform
import com.tapsioss.ripple.core.RippleClient
import com.tapsioss.ripple.core.RippleConfig
import java.util.UUID

/**
 * Spring-specific Ripple client implementation.
 * 
 * Provides event tracking optimized for Spring Boot applications.
 * Thread-safe and suitable for use as a singleton bean.
 * 
 * Example usage:
 * ```kotlin
 * @Configuration
 * class RippleConfiguration {
 *     @Bean
 *     fun rippleClient(): SpringRippleClient {
 *         val config = RippleConfig(
 *             apiKey = "your-api-key",
 *             endpoint = "https://api.example.com/events",
 *             adapters = AdapterConfig(
 *                 httpAdapter = WebClientAdapter(),
 *                 storageAdapter = FileStorageAdapter(),
 *                 loggerAdapter = Slf4jLoggerAdapter()
 *             )
 *         )
 *         return SpringRippleClient(config).apply { init() }
 *     }
 * }
 * ```
 * 
 * @param config Ripple configuration
 */
class SpringRippleClient(
    config: RippleConfig
) : RippleClient(config) {
    
    private val sessionId: String = UUID.randomUUID().toString()

    override fun getSessionId(): String = sessionId

    override fun getPlatform(): Platform = Platform(
        os = System.getProperty("os.name"),
        osVersion = System.getProperty("os.version"),
        device = null,
        manufacturer = null
    )
}
