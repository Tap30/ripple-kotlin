package cab.tapsi.ripple.core

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EventSpecsTest {
    @Test
    fun `isPredefinedEvent recognizes exact predefined names only`() {
        val baseEvent = Event(
            name = "product_viewed",
            payload = null,
            issuedAt = 0,
            metadata = null,
            platform = null
        )

        assertTrue(isPredefinedEvent(baseEvent))
        assertFalse(isPredefinedEvent(baseEvent.copy(name = "product")))
        assertFalse(isPredefinedEvent(baseEvent.copy(name = "custom.event")))
        assertTrue(isPredefinedEvent(baseEvent.copy(name = "order_reviewed")))
        assertFalse(isPredefinedEvent(baseEvent.copy(name = "coupon_redeemed")))
    }

    @Test
    fun `events namespace supports all event categories`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 1))
        val events = client.events

        val product = Product(
            price = Money(amount = 0.0, currency = ""),
            productId = ""
        )
        val order = Order(
            orderId = "",
            products = emptyList(),
            totalValue = Money(amount = 0.0, currency = "")
        )
        val checkoutOrder = CheckoutOrder(
            products = emptyList(),
            totalValue = Money(amount = 0.0, currency = "")
        )
        val checkout = Checkout(
            step = "0",
            order = checkoutOrder
        )
        val coupon = Coupon(
            amount = Money(amount = 0.0, currency = ""),
            code = ""
        )
        val payment = Payment(
            value = Money(amount = 0.0, currency = ""),
            method = "",
            paymentId = ""
        )
        val referral = Referral(referralCode = "")
        val incentive = Incentive(
            incentiveId = "",
            reward = Reward(amount = 0.0, unit = ""),
            type = ""
        )
        val challenge = Challenge(challengeId = "")
        val cart = Cart(products = emptyList())

        val methods = listOf<Pair<String, EventsNamespace.() -> Unit>>(
            "app_state_changed" to { appStateChanged(AppStateChangedPayload(newState = AppState.FOREGROUND)) },
            "product_clicked" to { productClicked(ProductPayload(product)) },
            "product_viewed" to { productViewed(ProductPayload(product)) },
            "product_shared" to { productShared(ProductSharePayload(product)) },
            "products_searched" to { productsSearched(ProductsSearchedPayload(query = "test")) },
            "product_list_viewed" to { productListViewed(ProductListViewedPayload(products = emptyList())) },
            "product_list_filtered" to {
                productListFiltered(
                    ProductListFilteredPayload(
                        filters = emptyList(),
                        sorts = emptyList(),
                        products = emptyList()
                    )
                )
            },
            "product_reviewed" to {
                productReviewed(
                    ProductReviewedPayload(
                        product = product,
                        reviewId = "1",
                        rating = 5.0
                    )
                )
            },
            "product_added_to_wishlist" to { productAddedToWishlist(ProductWishlistPayload(product)) },
            "product_removed_from_wishlist" to { productRemovedFromWishlist(ProductWishlistPayload(product)) },
            "product_added_to_cart" to { productAddedToCart(CartModificationPayload(product, cart)) },
            "product_removed_from_cart" to { productRemovedFromCart(CartModificationPayload(product, cart)) },
            "cart_viewed" to { cartViewed(CartPayload(cart)) },
            "cart_emptied" to { cartEmptied(CartPayload(cart)) },
            "checkout_started" to { checkoutStarted(CheckoutStartedPayload(checkout)) },
            "checkout_step_viewed" to { checkoutStepViewed(CheckoutStepPayload(checkout)) },
            "checkout_step_completed" to { checkoutStepCompleted(CheckoutStepPayload(checkout)) },
            "order_completed" to { orderCompleted(OrderCompletedPayload(order)) },
            "order_failed" to { orderFailed(OrderFailedPayload(order, reason = "err")) },
            "order_cancelled" to { orderCancelled(OrderCancelledPayload(order)) },
            "order_shipped" to { orderShipped(OrderShippedPayload(order)) },
            "order_refunded" to { orderRefunded(OrderRefundedPayload(order)) },
            "order_updated" to { orderUpdated(OrderUpdatedPayload(order)) },
            "order_product_fulfilled" to { orderProductFulfilled(OrderProductFulfilledPayload(order, product)) },
            "order_product_returned" to {
                orderProductReturned(
                    OrderProductReturnedPayload(
                        order = order,
                        product = product,
                        reason = "r",
                        totalReturnedValue = Money(amount = 0.0, currency = "")
                    )
                )
            },
            "order_fulfillment_status_updated" to {
                orderFulfillmentStatusUpdated(
                    OrderFulfillmentStatusUpdatedPayload(
                        order = order,
                        previousStatus = "",
                        newStatus = ""
                    )
                )
            },
            "order_reviewed" to { orderReviewed(OrderReviewedPayload(order, reviewId = "r", rating = 5.0)) },
            "coupon_entered" to { couponEntered(CouponEnteredRemovedPayload(coupon, checkout)) },
            "coupon_removed" to { couponRemoved(CouponEnteredRemovedPayload(coupon, checkout)) },
            "coupon_denied" to { couponDenied(CouponDeniedPayload(coupon, checkout, reason = "r")) },
            "promotion_viewed" to { promotionViewed(PromotionPayload(promotionId = "1")) },
            "promotion_clicked" to { promotionClicked(PromotionPayload(promotionId = "1")) },
            "payment_authorized" to { paymentAuthorized(PaymentAuthorizedPayload(payment)) },
            "payment_captured" to { paymentCaptured(PaymentCapturedPayload(payment)) },
            "payment_failed" to { paymentFailed(PaymentFailedPayload(payment)) },
            "payment_refunded" to {
                paymentRefunded(
                    PaymentRefundedPayload(
                        payment = payment,
                        returnedAmount = Money(amount = 0.0, currency = "")
                    )
                )
            },
            "referral_shared" to { referralShared(ReferralSharedPayload(referral)) },
            "referral_applied" to { referralApplied(ReferralAppliedPayload(referral)) },
            "incentive_granted" to { incentiveGranted(IncentiveGrantedPayload(incentive)) },
            "incentive_redeemed" to { incentiveRedeemed(IncentiveRedeemedPayload(incentive)) },
            "incentive_claimed" to { incentiveClaimed(IncentiveClaimedPayload(incentive)) },
            "incentive_expired" to { incentiveExpired(IncentiveExpiredPayload(incentive)) },
            "challenge_started" to { challengeStarted(ChallengeStartedPayload(challenge)) },
            "challenge_completed" to { challengeCompleted(ChallengeCompletedPayload(challenge)) },
            "challenge_step_completed" to { challengeStepCompleted(ChallengeStepCompletedPayload(challenge, step = "0")) }
        )

        methods.forEachIndexed { index, (eventName, call) ->
            events.call()

            eventually { http.requests.size == index + 1 }
            val event = http.requests[index].events.single()

            assertEquals(eventName, event.name)
            assertEquals(PREDEFINED_SCHEMA_VERSION, event.schemaVersion)
        }
    }

    @Test
    fun `predefined event encodes payload as json object and omits nulls`() {
        val payload = ScreenPayload(
            title = "Home",
            url = "https://example.test/home",
            keywords = listOf("food", "delivery"),
            campaign = Campaign(source = "app", medium = "push")
        )
            .toJsonPayload()

        assertEquals("Home", payload["title"]?.jsonPrimitive?.content)
        assertEquals("https://example.test/home", payload["url"]?.jsonPrimitive?.content)
        assertEquals("app", payload["campaign"]?.jsonObject?.get("source")?.jsonPrimitive?.content)
        assertFalse(payload["campaign"]!!.jsonObject.containsKey("name"))
    }

    @Test
    fun `predefined event preserves nested money category and custom properties`() {
        val product = Product(
            productId = "p1",
            productTitle = "Coffee",
            price = Money(amount = 12_500.0, currency = "IRR"),
            category = Category(id = "c1", title = "Drinks"),
            customProperties = JsonObject(mapOf("source" to JsonPrimitive("test")))
        )

        val payload = ProductPayload(product).toJsonPayload()
        val productJson = payload["product"]!!.jsonObject

        assertEquals("p1", productJson["productId"]?.jsonPrimitive?.content)
        assertEquals(12_500.0, productJson["price"]?.jsonObject?.get("amount")?.jsonPrimitive?.double)
        assertEquals("IRR", productJson["price"]?.jsonObject?.get("currency")?.jsonPrimitive?.content)
        assertEquals("Drinks", productJson["category"]?.jsonObject?.get("title")?.jsonPrimitive?.content)
        assertEquals("test", productJson["customProperties"]?.jsonObject?.get("source")?.jsonPrimitive?.content)
    }

    @Test
    fun `app state serializes with declared serial name`() {
        val payload = AppStateChangedPayload(newState = AppState.FOREGROUND, previousState = AppState.BACKGROUND)
            .toJsonPayload()

        assertEquals("foreground", payload["newState"]?.jsonPrimitive?.content)
        assertEquals("background", payload["previousState"]?.jsonPrimitive?.content)
    }

    @Test
    fun `identified user payload includes traits and custom properties`() {
        val payload = UserIdentifiedPayload(
            userId = "u1",
            traits = UserTraits(
                email = "u@example.test",
                customProperties = JsonObject(mapOf("tier" to JsonPrimitive("gold")))
            )
        ).toJsonPayload()

        assertEquals("u1", payload["userId"]?.jsonPrimitive?.content)
        assertEquals("u@example.test", payload["traits"]?.jsonObject?.get("email")?.jsonPrimitive?.content)
        assertEquals("gold", payload["traits"]?.jsonObject?.get("customProperties")?.jsonObject?.get("tier")?.jsonPrimitive?.content)
    }

    @Test
    fun `cart events encode cart object and custom properties`() {
        val product = Product(
            productId = "p1",
            price = Money(amount = 10.0, currency = "IRR")
        )
        val cart = Cart(
            cartId = "cart-1",
            products = listOf(product),
            customProperties = buildJsonObject { put("channel", "mobile") }
        )

        val modificationPayload = CartModificationPayload(
            product = product,
            cart = cart,
            customProperties = buildJsonObject { put("source", "button") }
        ).toJsonPayload()
        val viewedPayload = CartPayload(cart).toJsonPayload()

        assertEquals("cart-1", modificationPayload["cart"]?.jsonObject?.get("cartId")?.jsonPrimitive?.content)
        assertEquals("p1", modificationPayload["cart"]?.jsonObject?.get("products")?.jsonArray?.first()?.jsonObject?.get("productId")?.jsonPrimitive?.content)
        assertEquals("mobile", modificationPayload["cart"]?.jsonObject?.get("customProperties")?.jsonObject?.get("channel")?.jsonPrimitive?.content)
        assertEquals("button", modificationPayload["customProperties"]?.jsonObject?.get("source")?.jsonPrimitive?.content)
        assertEquals("cart-1", viewedPayload["cart"]?.jsonObject?.get("cartId")?.jsonPrimitive?.content)
        assertFalse(modificationPayload.containsKey("cartId"))
        assertFalse(viewedPayload.containsKey("products"))
    }

    @Test
    fun `address and coupon preserve custom properties`() {
        val address = Address(
            fullAddress = "Main St",
            city = "Tehran",
            customProperties = buildJsonObject { put("zone", "north") }
        )
        val coupon = Coupon(
            code = "SAVE",
            amount = Money(amount = 1000.0, currency = "IRR"),
            customProperties = buildJsonObject { put("campaign", "summer") }
        )

        val shippingPayload = Shipping(
            price = Money(amount = 500.0, currency = "IRR"),
            method = "standard",
            destination = address
        ).toJsonPayload()
        val couponPayload = CouponEnteredRemovedPayload(
            coupon = coupon,
            checkout = Checkout(
                order = CheckoutOrder(
                    products = emptyList(),
                    totalValue = Money(amount = 0.0, currency = "IRR")
                ),
                step = "1"
            )
        ).toJsonPayload()

        assertEquals("north", shippingPayload["destination"]?.jsonObject?.get("customProperties")?.jsonObject?.get("zone")?.jsonPrimitive?.content)
        assertEquals("summer", couponPayload["coupon"]?.jsonObject?.get("customProperties")?.jsonObject?.get("campaign")?.jsonPrimitive?.content)
    }
}
