package com.tapsioss.ripple.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

const val PREDEFINED_SCHEMA_VERSION: String = "1"

sealed interface PayloadConvertible

private val payloadJson = Json {
    encodeDefaults = false
}

internal inline fun <reified T : PayloadConvertible> T.toJsonPayload(): JsonObject {
    val element = payloadJson.encodeToJsonElement(serializer<T>(), this)
    require(element is JsonObject) {
        "Payload ${T::class.simpleName} must encode to a JSON object."
    }
    return element
}

@Serializable
data class Money(
    val amount: Long,
    val currency: String
) : PayloadConvertible

@Serializable
data class Category(val id: String, val title: String? = null) : PayloadConvertible

@Serializable
data class GeoLocation(
    val lat: Double,
    val long: Double
) : PayloadConvertible

@Serializable
data class Address(
    val fullAddress: String,
    val city: String,
    val location: GeoLocation? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Shipping(
    val price: Money,
    val method: String,
    val destination: Address,
    val origin: Address? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Coupon(
    val code: String,
    val amount: Money,
    val id: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Product(
    val productId: String,
    val price: Money,
    val productTitle: String? = null,
    val category: Category? = null,
    val skuId: String? = null,
    val vendor: String? = null,
    val discount: Money? = null,
    val quantity: Int? = null,
    val position: Int? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Order(
    val orderId: String,
    val products: List<Product>,
    val revenue: Money,
    val total: Money,
    val checkoutId: String? = null,
    val transactionId: String? = null,
    val cartId: String? = null,
    val totalDiscount: Money? = null,
    val subtotalValue: Money? = null,
    val shipping: Shipping? = null,
    val tax: Money? = null,
    val coupons: List<Coupon>? = null,
    val discount: Money? = null,
    val paymentMethod: String? = null,
    val incentives: List<Incentive>? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Cart(
    val cartId: String? = null,
    val products: List<Product>,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Checkout(
    val order: Order,
    val step: String,
    val checkoutId: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Pagination(
    val page: Int? = null,
    val pageSize: Int? = null,
    val totalPages: Int? = null,
    val limit: Int? = null
) : PayloadConvertible

@Serializable
data class Filter(
    val key: String,
    @Serializable(with = AnySerializer::class) val value: Any
) : PayloadConvertible

@Serializable
data class Sort(val key: String, val value: String) : PayloadConvertible

@Serializable
data class Payment(
    val paymentId: String,
    val method: String,
    val value: Money,
    val gateway: String? = null,
    val order: Order? = null,
    val orderId: String? = null,
    val amount: Money? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Campaign(
    val source: String,
    val medium: String,
    val name: String? = null,
    val term: String? = null,
    val content: String? = null
) : PayloadConvertible

@Serializable
data class Reward(
    val amount: Long,
    val unit: String
) : PayloadConvertible

@Serializable
data class Incentive(
    val incentiveId: String,
    val type: String,
    val reward: Reward,
    val incentiveTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Challenge(
    val challengeId: String,
    val challengeTitle: String? = null,
    val category: Category? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Referral(
    val referralCode: String,
    val referrerId: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
enum class AppState {
    @SerialName("opened")
    OPENED,

    @SerialName("closed")
    CLOSED,

    @SerialName("foreground")
    FOREGROUND,

    @SerialName("background")
    BACKGROUND
}

@Serializable
data class UserTraits(
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val username: String? = null,
    val age: Int? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: String? = null,
    val birthday: Int? = null,
    val createdAt: Int? = null,
    val address: Address? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class UserIdentifiedPayload(
    val userId: String,
    val traits: UserTraits
) : PayloadConvertible

@Serializable
data class ScreenPayload(
    val title: String,
    val url: String? = null,
    val pathname: String? = null,
    val referrer: String? = null,
    val search: String? = null,
    val keywords: List<String>? = null,
    val campaign: Campaign? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ClickedPayload(
    val elementId: String,
    val elementType: String? = null,
    val elementTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ViewedPayload(
    val elementId: String,
    val elementType: String? = null,
    val elementTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class AppStateChangedPayload(
    val newState: AppState,
    val previousState: AppState? = null
) : PayloadConvertible

@Serializable
data class ProductPayload(
    val product: Product,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductSharePayload(
    val product: Product,
    val sharingMethod: String? = null,
    val message: String? = null,
    val recipient: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductWishlistPayload(
    val product: Product,
    val wishlistId: String? = null,
    val referrer: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CartModificationPayload(
    val product: Product,
    val cart: Cart,
    val referrer: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductReviewedPayload(
    val product: Product,
    val reviewId: String,
    val rating: Long,
    val title: String? = null,
    val body: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CartPayload(
    val cart: Cart,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CheckoutPayload(
    val checkout: Checkout,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderPayload(
    val order: Order,
    val reason: String? = null,
    val issuer: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderProductPayload(
    val order: Order,
    val product: Product,
    val reason: String? = null,
    val refundMethod: String? = null,
    val totalReturnedValue: Money? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderFulfillmentStatusUpdatedPayload(
    val order: Order,
    val previousStatus: String,
    val newStatus: String,
    val reason: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductsSearchedPayload(
    val query: String,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductListViewedPayload(
    val products: List<Product>,
    val listId: String? = null,
    val category: Category? = null,
    val pagination: Pagination? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductListFilteredPayload(
    val products: List<Product>,
    val filters: List<Filter>,
    val sorts: List<Sort>,
    val listId: String? = null,
    val category: Category? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CouponCheckoutPayload(
    val coupon: Coupon,
    val checkout: Checkout,
    val reason: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CouponOrderPayload(
    val coupon: Coupon,
    val order: Order,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PromotionPayload(
    val promotionId: String,
    val promotionTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PaymentPayload(
    val payment: Payment,
    val reason: String? = null,
    val returnedAmount: Money? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ReferralPayload(
    val referral: Referral,
    val medium: String? = null,
    val flow: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class IncentivePayload(
    val incentive: Incentive,
    val sourceId: String? = null,
    val sourceTitle: String? = null,
    val redeemerId: String? = null,
    val redeemerTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ChallengePayload(
    val challenge: Challenge,
    val step: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

class EventsNamespace internal constructor(
    private val client: RippleClient<out RippleEvent, out RippleMetadata>
) {
    private inline fun <reified T : PayloadConvertible> track(
        name: String,
        payload: T
    ) = client.trackPredefined(name, payload.toJsonPayload())

    fun productClicked(payload: ProductPayload) = track("product_clicked", payload)
    fun productViewed(payload: ProductPayload) = track("product_viewed", payload)
    fun productShared(payload: ProductSharePayload) = track("product_shared", payload)
    fun productsSearched(payload: ProductsSearchedPayload) = track("products_searched", payload)
    fun productListViewed(payload: ProductListViewedPayload) = track("product_list_viewed", payload)
    fun productListFiltered(payload: ProductListFilteredPayload) = track("product_list_filtered", payload)
    fun productReviewed(payload: ProductReviewedPayload) = track("product_reviewed", payload)
    fun productAddedToWishlist(payload: ProductWishlistPayload) = track("product_added_to_wishlist", payload)
    fun productRemovedFromWishlist(payload: ProductWishlistPayload) = track("product_removed_from_wishlist", payload)
    fun productAddedToCart(payload: CartModificationPayload) = track("product_added_to_cart", payload)
    fun productRemovedFromCart(payload: CartModificationPayload) = track("product_removed_from_cart", payload)
    fun cartViewed(payload: CartPayload) = track("cart_viewed", payload)
    fun cartEmptied(payload: CartPayload) = track("cart_emptied", payload)
    fun checkoutStarted(payload: CheckoutPayload) = track("checkout_started", payload)
    fun checkoutStepViewed(payload: CheckoutPayload) = track("checkout_step_viewed", payload)
    fun checkoutStepCompleted(payload: CheckoutPayload) = track("checkout_step_completed", payload)
    fun orderCompleted(payload: OrderPayload) = track("order_completed", payload)
    fun orderFailed(payload: OrderPayload) = track("order_failed", payload)
    fun orderCancelled(payload: OrderPayload) = track("order_cancelled", payload)
    fun orderShipped(payload: OrderPayload) = track("order_shipped", payload)
    fun orderRefunded(payload: OrderPayload) = track("order_refunded", payload)
    fun orderUpdated(payload: OrderPayload) = track("order_updated", payload)
    fun orderProductFulfilled(payload: OrderProductPayload) = track("order_product_fulfilled", payload)
    fun orderProductReturned(payload: OrderProductPayload) = track("order_product_returned", payload)
    fun orderFulfillmentStatusUpdated(payload: OrderFulfillmentStatusUpdatedPayload) =
        track("order_fulfillment_status_updated", payload)
    fun couponEntered(payload: CouponCheckoutPayload) = track("coupon_entered", payload)
    fun couponRemoved(payload: CouponCheckoutPayload) = track("coupon_removed", payload)
    fun couponDenied(payload: CouponCheckoutPayload) = track("coupon_denied", payload)
    fun couponRedeemed(payload: CouponOrderPayload) = track("coupon_redeemed", payload)
    fun promotionViewed(payload: PromotionPayload) = track("promotion_viewed", payload)
    fun promotionClicked(payload: PromotionPayload) = track("promotion_clicked", payload)
    fun paymentAuthorized(payload: PaymentPayload) = track("payment_authorized", payload)
    fun paymentCaptured(payload: PaymentPayload) = track("payment_captured", payload)
    fun paymentFailed(payload: PaymentPayload) = track("payment_failed", payload)
    fun paymentRefunded(payload: PaymentPayload) = track("payment_refunded", payload)
    fun appStateChanged(payload: AppStateChangedPayload) = track("app_state_changed", payload)
    fun referralShared(payload: ReferralPayload) = track("referral_shared", payload)
    fun referralApplied(payload: ReferralPayload) = track("referral_applied", payload)
    fun incentiveGranted(payload: IncentivePayload) = track("incentive_granted", payload)
    fun incentiveRedeemed(payload: IncentivePayload) = track("incentive_redeemed", payload)
    fun incentiveClaimed(payload: IncentivePayload) = track("incentive_claimed", payload)
    fun incentiveExpired(payload: IncentivePayload) = track("incentive_expired", payload)
    fun challengeStarted(payload: ChallengePayload) = track("challenge_started", payload)
    fun challengeCompleted(payload: ChallengePayload) = track("challenge_completed", payload)
    fun challengeStepCompleted(payload: ChallengePayload) = track("challenge_step_completed", payload)
}
