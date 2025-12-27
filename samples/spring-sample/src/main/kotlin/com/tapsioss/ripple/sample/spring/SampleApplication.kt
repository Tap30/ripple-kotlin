package com.tapsioss.ripple.sample.spring

import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.spring.SpringRippleClient
import com.tapsioss.ripple.spring.adapters.logging.Slf4jLoggerAdapter
import com.tapsioss.ripple.spring.adapters.storage.FileStorageAdapter
import com.tapsioss.ripple.spring.adapters.webflux.WebClientAdapter
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class SampleApplication {

    @Bean
    fun rippleClient(): SpringRippleClient {
        val config = RippleConfig(
            apiKey = "your-api-key",
            endpoint = "https://api.example.com/events",
            adapters = AdapterConfig(
                httpAdapter = WebClientAdapter(),
                storageAdapter = FileStorageAdapter(),
                loggerAdapter = Slf4jLoggerAdapter(logLevel = LogLevel.INFO)
            )
        )
        return SpringRippleClient(config)
    }

    @Bean
    fun demo(rippleClient: SpringRippleClient) = CommandLineRunner {
        runBlocking {
            rippleClient.init()
            
            // Track server events
            rippleClient.setMetadata("service", "user-service")
            rippleClient.track("api_request", mapOf(
                "endpoint" to "/api/users",
                "method" to "GET",
                "duration" to 150
            ))
            
            rippleClient.flush()
        }
    }
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
