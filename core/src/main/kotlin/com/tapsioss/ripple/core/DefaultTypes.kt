package com.tapsioss.ripple.core

/**
 * Default event implementation for untyped usage.
 */
data class DefaultRippleEvent(
    override val name: String,
    private val payloadData: Map<String, Any>? = null
) : RippleEvent {
    override fun toPayload(): Map<String, Any>? = payloadData
}

/**
 * Default metadata implementation for untyped usage.
 */
data class DefaultRippleMetadata(
    private val data: Map<String, Any> = emptyMap()
) : RippleMetadata {
    override fun toMap() = data
}
