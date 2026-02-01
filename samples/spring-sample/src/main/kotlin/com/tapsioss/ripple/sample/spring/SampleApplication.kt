package com.tapsioss.ripple.sample.spring

import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.RippleEvent
import com.tapsioss.ripple.core.RippleMetadata
import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.spring.SpringRippleClient
import com.tapsioss.ripple.spring.adapters.logging.Slf4jLoggerAdapter
import com.tapsioss.ripple.spring.adapters.storage.FileStorageAdapter
import com.tapsioss.ripple.spring.adapters.webflux.WebClientAdapter
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

// Define type-safe events for Spring application
sealed class ServerEvent : RippleEvent {
    data class ApiRequest(
        val endpoint: String,
        val method: String,
        val duration: Long,
        val statusCode: Int
    ) : ServerEvent() {
        override val name = "api_request"
        override fun toPayload() = mapOf(
            "endpoint" to endpoint,
            "method" to method,
            "duration_ms" to duration,
            "status_code" to statusCode,
            "timestamp" to System.currentTimeMillis()
        )
    }
    
    data class UserAction(
        val userId: String,
        val action: String,
        val resource: String?
    ) : ServerEvent() {
        override val name = "user_action"
        override fun toPayload() = mapOf(
            "user_id" to userId,
            "action" to action,
            "resource" to (resource ?: "unknown")
        )
    }
    
    data class SystemEvent(
        val eventType: String,
        val severity: String,
        val message: String
    ) : ServerEvent() {
        override val name = "system_event"
        override fun toPayload() = mapOf(
            "event_type" to eventType,
            "severity" to severity,
            "message" to message
        )
    }
}

// Define type-safe metadata for Spring application
data class ServerMetadata(
    val service: String? = null,
    val environment: String? = null,
    val version: String? = null,
    val instanceId: String? = null
) : RippleMetadata {
    override fun toMap() = buildMap {
        service?.let { put("service", it) }
        environment?.let { put("environment", it) }
        version?.let { put("version", it) }
        instanceId?.let { put("instance_id", it) }
    }
}

@SpringBootApplication
class SampleApplication {

    private val logger = LoggerFactory.getLogger(SampleApplication::class.java)

    @Bean
    fun rippleClient(): SpringRippleClient<ServerEvent, ServerMetadata> {
        val config = RippleConfig(
            apiKey = "spring-demo-key",
            endpoint = "http://localhost:8080/events",
            adapters = AdapterConfig(
                httpAdapter = WebClientAdapter(),
                storageAdapter = FileStorageAdapter(),
                loggerAdapter = Slf4jLoggerAdapter(logLevel = LogLevel.INFO)
            )
        )
        return SpringRippleClient(config)
    }

    @Bean
    fun demo(rippleClient: SpringRippleClient<ServerEvent, ServerMetadata>) = CommandLineRunner {
        rippleClient.init()
        
        // Set global metadata
        val metadata = ServerMetadata(
            service = "user-service",
            environment = "development",
            version = "1.0.0",
            instanceId = "instance-${System.currentTimeMillis()}"
        )
        rippleClient.setMetadata(metadata)
        
        logger.info("Ripple client initialized with metadata: $metadata")
        
        // Track application startup
        rippleClient.track(ServerEvent.SystemEvent(
            eventType = "application_startup",
            severity = "info",
            message = "Spring application started successfully"
        ))
        
        // Simulate some API requests
        repeat(3) { i ->
            rippleClient.track(ServerEvent.ApiRequest(
                endpoint = "/api/users/$i",
                method = "GET",
                duration = (50..200).random().toLong(),
                statusCode = 200
            ))
        }
        
        // Track user actions
        rippleClient.track(ServerEvent.UserAction(
            userId = "user-123",
            action = "login",
            resource = "web_portal"
        ))
        
        rippleClient.flush()
        logger.info("Demo events tracked and flushed")
    }
}

@RestController
class DemoController(
    private val rippleClient: SpringRippleClient<ServerEvent, ServerMetadata>
) {
    
    private val logger = LoggerFactory.getLogger(DemoController::class.java)
    
    @GetMapping("/demo/track")
    fun trackDemo(): Map<String, Any?> {
        val startTime = System.currentTimeMillis()
        
        // Track the API request
        rippleClient.track(ServerEvent.ApiRequest(
            endpoint = "/demo/track",
            method = "GET",
            duration = System.currentTimeMillis() - startTime,
            statusCode = 200
        ))
        
        logger.info("Tracked demo API request")
        
        return mapOf(
            "message" to "Event tracked successfully",
            "queueSize" to rippleClient.getQueueSize(),
            "sessionId" to rippleClient.getSessionId()
        )
    }
    
    @PostMapping("/demo/user-action")
    fun trackUserAction(@RequestBody request: UserActionRequest): Map<String, Any?> {
        val startTime = System.currentTimeMillis()
        
        // Track user action with event-specific metadata
        val eventMetadata = ServerMetadata(instanceId = "api-handler")
        rippleClient.track(
            ServerEvent.UserAction(
                userId = request.userId,
                action = request.action,
                resource = request.resource
            ),
            eventMetadata
        )
        
        // Track the API request itself
        rippleClient.track(ServerEvent.ApiRequest(
            endpoint = "/demo/user-action",
            method = "POST",
            duration = System.currentTimeMillis() - startTime,
            statusCode = 200
        ))
        
        logger.info("Tracked user action: ${request.action} for user ${request.userId}")
        
        return mapOf(
            "message" to "User action tracked",
            "userId" to request.userId,
            "action" to request.action
        )
    }
    
    @GetMapping("/demo/status")
    fun getStatus(): Map<String, Any?> {
        return mapOf(
            "queueSize" to rippleClient.getQueueSize(),
            "sessionId" to rippleClient.getSessionId(),
            "metadata" to rippleClient.getMetadata()
        )
    }
}

data class UserActionRequest(
    val userId: String,
    val action: String,
    val resource: String?
)

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
