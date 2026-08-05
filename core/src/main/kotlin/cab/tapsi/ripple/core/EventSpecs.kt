package cab.tapsi.ripple.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer

const val PREDEFINED_SCHEMA_VERSION: String = "1"

enum class PredefinedEventName(val wireName: String) {
    USER_IDENTIFIED("user_identified"),
    SCREENED("screened"),
    APP_STATE_CHANGED("app_state_changed"),
    CLICKED("clicked"),
    VIEWED("viewed"),
    PRODUCT_CLICKED("product_clicked"),
    PRODUCT_VIEWED("product_viewed"),
    PRODUCT_SHARED("product_shared"),
    PRODUCTS_SEARCHED("products_searched"),
    PRODUCT_LIST_VIEWED("product_list_viewed"),
    PRODUCT_LIST_FILTERED("product_list_filtered"),
    PRODUCT_REVIEWED("product_reviewed"),
    PRODUCT_ADDED_TO_WISHLIST("product_added_to_wishlist"),
    PRODUCT_REMOVED_FROM_WISHLIST("product_removed_from_wishlist"),
    PRODUCT_ADDED_TO_CART("product_added_to_cart"),
    PRODUCT_REMOVED_FROM_CART("product_removed_from_cart"),
    CART_VIEWED("cart_viewed"),
    CART_EMPTIED("cart_emptied"),
    CHECKOUT_STARTED("checkout_started"),
    CHECKOUT_STEP_VIEWED("checkout_step_viewed"),
    CHECKOUT_STEP_COMPLETED("checkout_step_completed"),
    ORDER_COMPLETED("order_completed"),
    ORDER_FAILED("order_failed"),
    ORDER_CANCELLED("order_cancelled"),
    ORDER_SHIPPED("order_shipped"),
    ORDER_REFUNDED("order_refunded"),
    ORDER_UPDATED("order_updated"),
    ORDER_PRODUCT_FULFILLED("order_product_fulfilled"),
    ORDER_PRODUCT_RETURNED("order_product_returned"),
    ORDER_FULFILLMENT_STATUS_UPDATED("order_fulfillment_status_updated"),
    ORDER_REVIEWED("order_reviewed"),
    COUPON_ENTERED("coupon_entered"),
    COUPON_REMOVED("coupon_removed"),
    COUPON_DENIED("coupon_denied"),
    PROMOTION_VIEWED("promotion_viewed"),
    PROMOTION_CLICKED("promotion_clicked"),
    PAYMENT_AUTHORIZED("payment_authorized"),
    PAYMENT_CAPTURED("payment_captured"),
    PAYMENT_FAILED("payment_failed"),
    PAYMENT_REFUNDED("payment_refunded"),
    REFERRAL_SHARED("referral_shared"),
    REFERRAL_APPLIED("referral_applied"),
    INCENTIVE_GRANTED("incentive_granted"),
    INCENTIVE_REDEEMED("incentive_redeemed"),
    INCENTIVE_CLAIMED("incentive_claimed"),
    INCENTIVE_EXPIRED("incentive_expired"),
    CHALLENGE_STARTED("challenge_started"),
    CHALLENGE_COMPLETED("challenge_completed"),
    CHALLENGE_STEP_COMPLETED("challenge_step_completed")
}

/** Returns true when [event] has one of the SDK's predefined event names. */
fun isPredefinedEvent(event: Event): Boolean =
    PredefinedEventName.entries.any { it.wireName == event.name }

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
    val quantity: Int? = null,
    val position: Int? = null,
    val customProperties: JsonObject? = null
) : PayloadConvertible

@Serializable
data class Order(
    val orderId: String,
    val products: List<Product>,
    val checkoutId: String? = null,
    val transactionId: String? = null,
    val cartId: String? = null,
    val totalValue: Money,
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
data class Pagination(
    val page: Int,
    val limit: Int
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
    val rating: Double,
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
data class OrderReviewedPayload(
    val order: Order,
    val reviewId: String,
    val rating: Double,
    val title: String? = null,
    val body: String? = null,
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

class EventsNamespace internal constructor(
    private val client: RippleClient<out RippleEvent, out RippleMetadata>
) {
    private inline fun <reified T : PayloadConvertible> track(
        name: PredefinedEventName,
        payload: T
    ) = client.trackPredefined(name, payload.toJsonPayload())

    fun productClicked(payload: ProductPayload) = track(PredefinedEventName.PRODUCT_CLICKED, payload)
    fun productViewed(payload: ProductPayload) = track(PredefinedEventName.PRODUCT_VIEWED, payload)
    fun productShared(payload: ProductSharePayload) = track(PredefinedEventName.PRODUCT_SHARED, payload)
    fun productsSearched(payload: ProductsSearchedPayload) = track(PredefinedEventName.PRODUCTS_SEARCHED, payload)
    fun productListViewed(payload: ProductListViewedPayload) = track(PredefinedEventName.PRODUCT_LIST_VIEWED, payload)
    fun productListFiltered(payload: ProductListFilteredPayload) = track(PredefinedEventName.PRODUCT_LIST_FILTERED, payload)
    fun productReviewed(payload: ProductReviewedPayload) = track(PredefinedEventName.PRODUCT_REVIEWED, payload)
    fun productAddedToWishlist(payload: ProductWishlistPayload) = track(PredefinedEventName.PRODUCT_ADDED_TO_WISHLIST, payload)
    fun productRemovedFromWishlist(payload: ProductWishlistPayload) = track(PredefinedEventName.PRODUCT_REMOVED_FROM_WISHLIST, payload)
    fun productAddedToCart(payload: CartModificationPayload) = track(PredefinedEventName.PRODUCT_ADDED_TO_CART, payload)
    fun productRemovedFromCart(payload: CartModificationPayload) = track(PredefinedEventName.PRODUCT_REMOVED_FROM_CART, payload)
    fun cartViewed(payload: CartPayload) = track(PredefinedEventName.CART_VIEWED, payload)
    fun cartEmptied(payload: CartPayload) = track(PredefinedEventName.CART_EMPTIED, payload)
    fun checkoutStarted(payload: CheckoutStartedPayload) = track(PredefinedEventName.CHECKOUT_STARTED, payload)
    fun checkoutStepViewed(payload: CheckoutStepPayload) = track(PredefinedEventName.CHECKOUT_STEP_VIEWED, payload)
    fun checkoutStepCompleted(payload: CheckoutStepPayload) = track(PredefinedEventName.CHECKOUT_STEP_COMPLETED, payload)
    fun orderCompleted(payload: OrderCompletedPayload) = track(PredefinedEventName.ORDER_COMPLETED, payload)
    fun orderFailed(payload: OrderFailedPayload) = track(PredefinedEventName.ORDER_FAILED, payload)
    fun orderCancelled(payload: OrderCancelledPayload) = track(PredefinedEventName.ORDER_CANCELLED, payload)
    fun orderShipped(payload: OrderShippedPayload) = track(PredefinedEventName.ORDER_SHIPPED, payload)
    fun orderRefunded(payload: OrderRefundedPayload) = track(PredefinedEventName.ORDER_REFUNDED, payload)
    fun orderUpdated(payload: OrderUpdatedPayload) = track(PredefinedEventName.ORDER_UPDATED, payload)
    fun orderProductFulfilled(payload: OrderProductFulfilledPayload) = track(PredefinedEventName.ORDER_PRODUCT_FULFILLED, payload)
    fun orderProductReturned(payload: OrderProductReturnedPayload) = track(PredefinedEventName.ORDER_PRODUCT_RETURNED, payload)
    fun orderFulfillmentStatusUpdated(payload: OrderFulfillmentStatusUpdatedPayload) =
        track(PredefinedEventName.ORDER_FULFILLMENT_STATUS_UPDATED, payload)
    fun orderReviewed(payload: OrderReviewedPayload) = track(PredefinedEventName.ORDER_REVIEWED, payload)
    fun couponEntered(payload: CouponEnteredRemovedPayload) = track(PredefinedEventName.COUPON_ENTERED, payload)
    fun couponRemoved(payload: CouponEnteredRemovedPayload) = track(PredefinedEventName.COUPON_REMOVED, payload)
    fun couponDenied(payload: CouponDeniedPayload) = track(PredefinedEventName.COUPON_DENIED, payload)
    fun promotionViewed(payload: PromotionPayload) = track(PredefinedEventName.PROMOTION_VIEWED, payload)
    fun promotionClicked(payload: PromotionPayload) = track(PredefinedEventName.PROMOTION_CLICKED, payload)
    fun paymentAuthorized(payload: PaymentAuthorizedPayload) = track(PredefinedEventName.PAYMENT_AUTHORIZED, payload)
    fun paymentCaptured(payload: PaymentCapturedPayload) = track(PredefinedEventName.PAYMENT_CAPTURED, payload)
    fun paymentFailed(payload: PaymentFailedPayload) = track(PredefinedEventName.PAYMENT_FAILED, payload)
    fun paymentRefunded(payload: PaymentRefundedPayload) = track(PredefinedEventName.PAYMENT_REFUNDED, payload)
    fun appStateChanged(payload: AppStateChangedPayload) = track(PredefinedEventName.APP_STATE_CHANGED, payload)
    fun referralShared(payload: ReferralSharedPayload) = track(PredefinedEventName.REFERRAL_SHARED, payload)
    fun referralApplied(payload: ReferralAppliedPayload) = track(PredefinedEventName.REFERRAL_APPLIED, payload)
    fun incentiveGranted(payload: IncentiveGrantedPayload) = track(PredefinedEventName.INCENTIVE_GRANTED, payload)
    fun incentiveRedeemed(payload: IncentiveRedeemedPayload) = track(PredefinedEventName.INCENTIVE_REDEEMED, payload)
    fun incentiveClaimed(payload: IncentiveClaimedPayload) = track(PredefinedEventName.INCENTIVE_CLAIMED, payload)
    fun incentiveExpired(payload: IncentiveExpiredPayload) = track(PredefinedEventName.INCENTIVE_EXPIRED, payload)
    fun challengeStarted(payload: ChallengeStartedPayload) = track(PredefinedEventName.CHALLENGE_STARTED, payload)
    fun challengeCompleted(payload: ChallengeCompletedPayload) = track(PredefinedEventName.CHALLENGE_COMPLETED, payload)
    fun challengeStepCompleted(payload: ChallengeStepCompletedPayload) = track(PredefinedEventName.CHALLENGE_STEP_COMPLETED, payload)
}
