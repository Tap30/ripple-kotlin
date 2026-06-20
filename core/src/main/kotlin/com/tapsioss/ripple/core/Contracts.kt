package com.tapsioss.ripple.core

import kotlinx.serialization.json.JsonObject

/**
 * Interface for type-safe event tracking.
 * 
 * Implement this interface to define strongly-typed events with
 * compile-time validation of event structure.
 * 
 * Example:
 * ```kotlin
 * sealed class AppEvent : RippleEvent {
 *     data class UserLogin(val email: String, val method: String) : AppEvent() {
 *         override val name = "user.login"
 *         override fun getPayload() = buildJsonObject {
 *             put("email", email)
 *             put("method", method)
 *         }
 *     }
 *     
 *     data class Purchase(val orderId: String, val amount: Double) : AppEvent() {
 *         override val name = "purchase"
 *         override fun getPayload() = buildJsonObject {
 *             put("orderId", orderId)
 *             put("amount", amount)
 *         }
 *     }
 * }
 * 
 * // Usage
 * client.track(AppEvent.UserLogin("user@example.com", "google"))
 * ```
 */
interface RippleEvent {
    /** Event name/identifier */
    val name: String

    /** Optional schema version for versioned event contracts. */
    val schemaVersion: String?
        get() = null

    fun getPayload(): JsonObject?
}

/**
 * Interface for type-safe metadata.
 * 
 * Implement this interface to define strongly-typed metadata with
 * compile-time validation.
 * 
 * Example:
 * ```kotlin
 * data class AppMetadata(
 *     val userId: String? = null,
 *     val version: String? = null,
 *     val environment: String? = null
 * ) : RippleMetadata {
 *     override fun toMap(): Map<String, Any> = buildMap {
 *         userId?.let { put("userId", it) }
 *         version?.let { put("version", it) }
 *         environment?.let { put("environment", it) }
 *     }
 * }
 * 
 * // Usage
 * client.setMetadata(AppMetadata(userId = "123", version = "1.0.0"))
 * client.track(event, AppMetadata(environment = "prod"))
 * ```
 */
interface RippleMetadata {
    /** Convert metadata to map */
    fun toMap(): Map<String, Any>
}
