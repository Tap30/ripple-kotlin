package com.tapsioss.ripple.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Serializer for Any type to support dynamic payload/metadata values.
 */
object AnySerializer : KSerializer<Any> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Any")

    override fun serialize(encoder: Encoder, value: Any) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(value.toJsonElement())
    }

    override fun deserialize(decoder: Decoder): Any {
        val jsonDecoder = decoder as JsonDecoder
        return jsonDecoder.decodeJsonElement().toAny()
    }

    private fun Any?.toJsonElement(): JsonElement = when (this) {
        null -> JsonNull
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(this.map { (k, v) -> k.toString() to v.toJsonElement() }.toMap())
        is List<*> -> JsonArray(this.map { it.toJsonElement() })
        else -> JsonPrimitive(this.toString())
    }

    private fun JsonElement.toAny(): Any = when (this) {
        is JsonNull -> "null"
        is JsonPrimitive -> when {
            isString -> content
            content == "true" || content == "false" -> content.toBoolean()
            content.contains('.') -> content.toDouble()
            else -> content.toLongOrNull() ?: content
        }
        is JsonObject -> this.map { (k, v) -> k to v.toAny() }.toMap()
        is JsonArray -> this.map { it.toAny() }
    }
}
