package com.tapsioss.ripple.core

import kotlinx.serialization.json.JsonObject

/**
 * Default event implementation for untyped usage.
 */
data class DefaultRippleEvent(
    override val name: String,
    private val payloadData: JsonObject? = null
) : RippleEvent {
    override fun getPayload(): JsonObject? = payloadData
}

/**
 * Default metadata implementation for untyped usage.
 */
data class DefaultRippleMetadata(
    private val data: Map<String, Any> = emptyMap()
) : RippleMetadata {
    override fun toMap() = data
}
