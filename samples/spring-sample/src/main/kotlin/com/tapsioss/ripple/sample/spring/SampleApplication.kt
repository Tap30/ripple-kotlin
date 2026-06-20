package com.tapsioss.ripple.sample.spring

import com.tapsioss.ripple.core.*
import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.spring.SpringRippleClient
import com.tapsioss.ripple.spring.adapters.logging.Slf4jLoggerAdapter
import com.tapsioss.ripple.spring.adapters.storage.FileStorageAdapter
import com.tapsioss.ripple.spring.adapters.webflux.WebClientAdapter
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
        override fun getPayload() = buildJsonObject {
            put("endpoint", endpoint)
            put("method", method)
            put("duration_ms", duration)
            put("status_code", statusCode)
            put("timestamp", System.currentTimeMillis())
        }
    }
    
    data class UserAction(
        val userId: String,
        val action: String,
        val resource: String?
    ) : ServerEvent() {
        override val name = "user_action"
        override fun getPayload() = buildJsonObject {
            put("user_id", userId)
            put("action", action)
            put("resource", resource ?: "unknown")
        }
    }
    
    data class SystemEvent(
        val eventType: String,
        val severity: String,
        val message: String
    ) : ServerEvent() {
        override val name = "system_event"
        override fun getPayload() = buildJsonObject {
            put("event_type", eventType)
            put("severity", severity)
            put("message", message)
        }
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
        trackPredefinedDemo(rippleClient, "startup")
        
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
            "anonymousId" to rippleClient.getAnonymousId(),
            "userId" to rippleClient.getUserId()
        )
    }
    
    @PostMapping("/demo/user-action")
    fun trackUserAction(@RequestBody request: UserActionRequest): Map<String, Any?> {
        val startTime = System.currentTimeMillis()
        
        // Track user action with event-specific metadata
        val eventMetadata = ServerMetadata(instanceId = "api-handler")
        rippleClient.setMetadata(eventMetadata)
        rippleClient.track(
            ServerEvent.UserAction(
                userId = request.userId,
                action = request.action,
                resource = request.resource
            )
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
            "anonymousId" to rippleClient.getAnonymousId(),
            "userId" to rippleClient.getUserId(),
            "metadata" to rippleClient.getMetadata()
        )
    }

    @PostMapping("/demo/predefined")
    fun trackPredefined(): Map<String, Any?> {
        trackPredefinedDemo(rippleClient, "api")
        rippleClient.flush()

        logger.info("Tracked predefined demo events")

        return mapOf(
            "message" to "Predefined events tracked",
            "queueSize" to rippleClient.getQueueSize(),
            "anonymousId" to rippleClient.getAnonymousId(),
            "userId" to rippleClient.getUserId()
        )
    }
}

data class UserActionRequest(
    val userId: String,
    val action: String,
    val resource: String?
)

private fun trackPredefinedDemo(
    rippleClient: SpringRippleClient<ServerEvent, ServerMetadata>,
    source: String
) {
    val product = Product(
        productId = "spring-sku-1",
        productTitle = "Spring demo subscription",
        price = Money(amount = 990_000, currency = "IRR"),
        category = Category(id = "subscription", title = "Subscription"),
        quantity = 1,
        customProperties = buildJsonObject {
            put("source", source)
            put("sample", "spring")
        }
    )
    val coupon = Coupon(
        code = "SPRING10",
        amount = Money(amount = 99_000, currency = "IRR")
    )
    val order = Order(
        orderId = "spring-order-${System.currentTimeMillis()}",
        products = listOf(product),
        revenue = Money(amount = 891_000, currency = "IRR"),
        total = Money(amount = 891_000, currency = "IRR"),
        cartId = "spring-cart",
        coupons = listOf(coupon),
        paymentMethod = "wallet"
    )
    val checkout = Checkout(
        order = order,
        step = "payment",
        checkoutId = "spring-checkout"
    )
    val payment = Payment(
        paymentId = "spring-payment-${System.currentTimeMillis()}",
        method = "wallet",
        value = Money(amount = 891_000, currency = "IRR"),
        orderId = order.orderId
    )

    rippleClient.identify(
        userId = "spring-user-123",
        traits = UserTraits(
            email = "spring-user@example.com",
            fullName = "Spring Demo User",
            customProperties = buildJsonObject {
                put("source", source)
            }
        )
    )
    rippleClient.screen(
        ScreenPayload(
            title = "Spring demo",
            pathname = "/demo/predefined",
            campaign = Campaign(source = "sample", medium = "server", name = "spring-demo")
        )
    )
    rippleClient.clicked(ClickedPayload(elementId = "spring_predefined_demo", elementType = "endpoint"))
    rippleClient.viewed(ViewedPayload(elementId = "spring_product_summary", elementType = "product"))
    rippleClient.events.appStateChanged(AppStateChangedPayload(newState = AppState.FOREGROUND))
    rippleClient.events.productsSearched(ProductsSearchedPayload(query = "subscription"))
    rippleClient.events.productListViewed(
        ProductListViewedPayload(
            products = listOf(product),
            listId = "spring_recommendations",
            pagination = Pagination(page = 1, pageSize = 10, totalPages = 1)
        )
    )
    rippleClient.events.productListFiltered(
        ProductListFilteredPayload(
            products = listOf(product),
            filters = listOf(Filter("billing_cycle", "monthly")),
            sorts = listOf(Sort("created_at", "desc")),
            listId = "spring_recommendations"
        )
    )
    rippleClient.events.productViewed(ProductPayload(product))
    val cart = Cart(cartId = "spring-cart", products = listOf(product))
    rippleClient.events.productAddedToCart(CartModificationPayload(product, cart))
    rippleClient.events.cartViewed(CartPayload(cart))
    rippleClient.events.checkoutStarted(CheckoutPayload(checkout))
    rippleClient.events.checkoutStepCompleted(CheckoutPayload(checkout))
    rippleClient.events.couponEntered(CouponCheckoutPayload(coupon, checkout))
    rippleClient.events.couponRedeemed(CouponOrderPayload(coupon, order))
    rippleClient.events.paymentAuthorized(PaymentPayload(payment))
    rippleClient.events.paymentCaptured(PaymentPayload(payment))
    rippleClient.events.orderCompleted(OrderPayload(order))
    rippleClient.events.promotionClicked(PromotionPayload("spring-promo", promotionTitle = "Spring launch"))
    rippleClient.events.referralApplied(
        ReferralPayload(
            referral = Referral(referralCode = "SPRING-DEMO", referrerId = "spring-user-123"),
            medium = "server",
            flow = source
        )
    )
    rippleClient.events.incentiveGranted(
        IncentivePayload(
            incentive = Incentive(
                incentiveId = "spring-incentive",
                type = "credit",
                reward = Reward(amount = 50_000, unit = "IRR")
            ),
            sourceId = order.orderId,
            sourceTitle = "Order completion"
        )
    )
    rippleClient.events.challengeCompleted(
        ChallengePayload(
            challenge = Challenge(challengeId = "spring-challenge", challengeTitle = "Server onboarding")
        )
    )
}

fun main(args: Array<String>) {
    runApplication<SampleApplication>(*args)
}
