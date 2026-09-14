package cab.tapsi.ripple.core

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
    val amount: Double,
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
    val arrival: Arrival? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Arrival(
    val absoluteArriveAt: Long? = null,
    val rangeArriveAt: List<Long>? = null
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
    val quantity: Double? = null,
    val position: Double? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Order(
    val orderId: String,
    val products: List<Product>,
    val totalValue: Money,
    val checkoutId: String? = null,
    val transactionId: String? = null,
    val cartId: String? = null,
    val totalDiscount: Money? = null,
    val subtotalValue: Money? = null,
    val shipping: Shipping? = null,
    val tax: Money? = null,
    val paymentMethod: String? = null,
    val incentives: List<Incentive>? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

/**
 * The order details collected during checkout.
 *
 * Checkout, transaction, and order identifiers are assigned after checkout, so they are not
 * part of this representation.
 */
@Serializable
data class CheckoutOrder(
    val products: List<Product>,
    val totalValue: Money,
    val cartId: String? = null,
    val totalDiscount: Money? = null,
    val subtotalValue: Money? = null,
    val shipping: Shipping? = null,
    val tax: Money? = null,
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
    val order: CheckoutOrder,
    val step: String,
    val checkoutId: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Pagination(
    val page: Double,
    val limit: Double
) : PayloadConvertible

@Serializable
data class Filter(
    val key: String,
    val value: String
) : PayloadConvertible

@Serializable
data class Sort(val key: String, val value: SortDirection) : PayloadConvertible

@Serializable
enum class SortDirection {
    @SerialName("asc") ASC,
    @SerialName("dsc") DSC
}

@Serializable
data class Payment(
    val paymentId: String,
    val method: String,
    val value: Money,
    val gateway: String? = null,
    val order: Order? = null,
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
    val amount: Double,
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
    @Deprecated(
        message = "Foreground app state is deprecated. Use OPENED or CLOSED instead.",
        replaceWith = ReplaceWith("AppState.OPENED")
    )
    FOREGROUND,

    @SerialName("background")
    @Deprecated(
        message = "Background app state is deprecated. Use OPENED or CLOSED instead.",
        replaceWith = ReplaceWith("AppState.CLOSED")
    )
    BACKGROUND
}

@Serializable
data class UserTraits(
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val username: String? = null,
    val age: Double? = null,
    val email: String? = null,
    val phone: String? = null,
    val gender: Gender? = null,
    val birthday: Long? = null,
    val registeredAt: Long? = null,
    val address: Address? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
enum class Gender {
    @SerialName("male") MALE,
    @SerialName("female") FEMALE,
    @SerialName("other") OTHER
}

@Serializable
data class UserIdentifiedPayload(
    val userId: String,
    val traits: UserTraits
) : PayloadConvertible

@Serializable
data class ScreenPayload(
    val title: String,
    val url: String,
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
data class ProductClickedPayload(
    val product: Product,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductViewedPayload(
    val product: Product,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ProductSharedPayload(
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
    val rating: Double,
    val title: String? = null,
    val body: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CartViewedPayload(
    val cart: Cart,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CartEmptiedPayload(
    val cart: Cart,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CheckoutStartedPayload(
    val checkout: Checkout,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CheckoutStepPayload(
    val checkout: Checkout,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderCompletedPayload(
    val order: Order,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderFailedPayload(
    val order: Order,
    val reason: String,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderCancelledPayload(
    val order: Order,
    val issuer: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderShippedPayload(
    val order: Order,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderRefundedPayload(
    val order: Order,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderUpdatedPayload(
    val order: Order,
    val reason: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderProductFulfilledPayload(
    val order: Order,
    val product: Product,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderProductReturnedPayload(
    val order: Order,
    val product: Product,
    val reason: String,
    val refundMethod: String? = null,
    val totalReturnedValue: Money,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderFulfillmentStatusUpdatedPayload(
    val order: Order,
    val previousStatus: String? = null,
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
data class CouponEnteredRemovedPayload(
    val coupon: Coupon,
    val checkout: Checkout,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class CouponDeniedPayload(
    val coupon: Coupon,
    val checkout: Checkout,
    val reason: String,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PromotionPayload(
    val promotionId: String,
    val promotionTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PaymentAuthorizedPayload(
    val payment: Payment,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PaymentCapturedPayload(
    val payment: Payment,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PaymentFailedPayload(
    val payment: Payment,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class PaymentRefundedPayload(
    val payment: Payment,
    val reason: String? = null,
    val returnedAmount: Money,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ReferralSharedPayload(
    val referral: Referral,
    val medium: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ReferralAppliedPayload(
    val referral: Referral,
    val flow: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class IncentiveGrantedPayload(
    val incentive: Incentive,
    val sourceId: String? = null,
    val sourceTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class IncentiveRedeemedPayload(
    val incentive: Incentive,
    val redeemerId: String? = null,
    val redeemerTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class IncentiveClaimedPayload(
    val incentive: Incentive,
    val sourceId: String? = null,
    val sourceTitle: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class IncentiveExpiredPayload(
    val incentive: Incentive,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ChallengeStartedPayload(
    val challenge: Challenge,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ChallengeCompletedPayload(
    val challenge: Challenge,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class ChallengeStepCompletedPayload(
    val challenge: Challenge,
    val step: String,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class OrderReviewedPayload(
    val order: Order,
    val reviewId: String,
    val rating: Double,
    val title: String? = null,
    val body: String? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

class EventsNamespace internal constructor(
    private val client: RippleClient<out RippleEvent, out RippleMetadata>,
    private val logger: cab.tapsi.ripple.core.adapters.LoggerAdapter
) {
    private inline fun <reified T : PayloadConvertible> track(
        name: String,
        payload: T
    ) = client.trackPredefined(name, payload.toJsonPayload())

    fun productClicked(payload: ProductClickedPayload) = track("product_clicked", payload)
    fun productViewed(payload: ProductViewedPayload) = track("product_viewed", payload)
    fun productShared(payload: ProductSharedPayload) = track("product_shared", payload)
    fun productsSearched(payload: ProductsSearchedPayload) = track("products_searched", payload)
    fun productListViewed(payload: ProductListViewedPayload) = track("product_list_viewed", payload)
    fun productListFiltered(payload: ProductListFilteredPayload) = track("product_list_filtered", payload)
    fun productReviewed(payload: ProductReviewedPayload) = track("product_reviewed", payload)
    fun productAddedToWishlist(payload: ProductWishlistPayload) = track("product_added_to_wishlist", payload)
    fun productRemovedFromWishlist(payload: ProductWishlistPayload) = track("product_removed_from_wishlist", payload)
    fun productAddedToCart(payload: CartModificationPayload) = track("product_added_to_cart", payload)
    fun productRemovedFromCart(payload: CartModificationPayload) = track("product_removed_from_cart", payload)
    fun cartViewed(payload: CartViewedPayload) = track("cart_viewed", payload)
    fun cartEmptied(payload: CartEmptiedPayload) = track("cart_emptied", payload)
    fun checkoutStarted(payload: CheckoutStartedPayload) = track("checkout_started", payload)
    fun checkoutStepViewed(payload: CheckoutStepPayload) = track("checkout_step_viewed", payload)
    fun checkoutStepCompleted(payload: CheckoutStepPayload) = track("checkout_step_completed", payload)
    fun orderCompleted(payload: OrderCompletedPayload) = track("order_completed", payload)
    fun orderFailed(payload: OrderFailedPayload) = track("order_failed", payload)
    fun orderCancelled(payload: OrderCancelledPayload) = track("order_cancelled", payload)
    fun orderShipped(payload: OrderShippedPayload) = track("order_shipped", payload)
    fun orderRefunded(payload: OrderRefundedPayload) = track("order_refunded", payload)
    fun orderUpdated(payload: OrderUpdatedPayload) = track("order_updated", payload)
    fun orderProductFulfilled(payload: OrderProductFulfilledPayload) = track("order_product_fulfilled", payload)
    fun orderProductReturned(payload: OrderProductReturnedPayload) = track("order_product_returned", payload)
    fun orderFulfillmentStatusUpdated(payload: OrderFulfillmentStatusUpdatedPayload) =
        track("order_fulfillment_status_updated", payload)
    fun orderReviewed(payload: OrderReviewedPayload) = track("order_reviewed", payload)
    fun couponEntered(payload: CouponEnteredRemovedPayload) = track("coupon_entered", payload)
    fun couponRemoved(payload: CouponEnteredRemovedPayload) = track("coupon_removed", payload)
    fun couponDenied(payload: CouponDeniedPayload) = track("coupon_denied", payload)
    fun promotionViewed(payload: PromotionPayload) = track("promotion_viewed", payload)
    fun promotionClicked(payload: PromotionPayload) = track("promotion_clicked", payload)
    fun paymentAuthorized(payload: PaymentAuthorizedPayload) = track("payment_authorized", payload)
    fun paymentCaptured(payload: PaymentCapturedPayload) = track("payment_captured", payload)
    fun paymentFailed(payload: PaymentFailedPayload) = track("payment_failed", payload)
    fun paymentRefunded(payload: PaymentRefundedPayload) = track("payment_refunded", payload)
    @Suppress("DEPRECATION")
    fun appStateChanged(payload: AppStateChangedPayload) {
        when (payload.newState) {
            AppState.FOREGROUND -> logger.warn(
                "AppState \"foreground\" is deprecated. Use \"opened\" or \"closed\" instead."
            )
            AppState.BACKGROUND -> logger.warn(
                "AppState \"background\" is deprecated. Use \"opened\" or \"closed\" instead."
            )
            else -> Unit
        }
        track("app_state_changed", payload)
    }
    fun referralShared(payload: ReferralSharedPayload) = track("referral_shared", payload)
    fun referralApplied(payload: ReferralAppliedPayload) = track("referral_applied", payload)
    fun incentiveGranted(payload: IncentiveGrantedPayload) = track("incentive_granted", payload)
    fun incentiveRedeemed(payload: IncentiveRedeemedPayload) = track("incentive_redeemed", payload)
    fun incentiveClaimed(payload: IncentiveClaimedPayload) = track("incentive_claimed", payload)
    fun incentiveExpired(payload: IncentiveExpiredPayload) = track("incentive_expired", payload)
    fun challengeStarted(payload: ChallengeStartedPayload) = track("challenge_started", payload)
    fun challengeCompleted(payload: ChallengeCompletedPayload) = track("challenge_completed", payload)
    fun challengeStepCompleted(payload: ChallengeStepCompletedPayload) = track("challenge_step_completed", payload)
}
