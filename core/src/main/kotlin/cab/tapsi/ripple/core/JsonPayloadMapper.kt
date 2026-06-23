package cab.tapsi.ripple.core

import kotlinx.serialization.json.*

fun Map<String, Any>.toJsonObject(): JsonObject {
    return JsonObject(mapValues { (_, value) -> value.toJsonElement() })
}

internal fun JsonObject.toPayloadMap(): Map<String, Any> {
    return mapNotNull { (key, value) ->
        if (value is JsonNull) null else key to value.toPayloadValue()
    }.toMap()
}

private fun Any?.toJsonElement(): JsonElement = when (this) {
    null -> JsonNull
    is JsonElement -> this
    is String -> JsonPrimitive(this)
    is Number -> JsonPrimitive(this)
    is Boolean -> JsonPrimitive(this)
    is Map<*, *> -> JsonObject(
        mapNotNull { (key, value) ->
            key?.toString()?.let { it to value.toJsonElement() }
        }.toMap()
    )
    is Iterable<*> -> JsonArray(map { it.toJsonElement() })
    is Array<*> -> JsonArray(map { it.toJsonElement() })
    else -> JsonPrimitive(toString())
}

private fun JsonElement.toPayloadValue(): Any = when (this) {
    is JsonObject -> toPayloadMap()

    is JsonArray -> mapNotNull { element ->
        if (element is JsonNull) null else element.toPayloadValue()
    }

    is JsonPrimitive -> when {
        isString -> content
        content == "true" || content == "false" -> content.toBoolean()
        content.contains('.') || content.contains('e', ignoreCase = true) -> content.toDouble()
        else -> content.toLongOrNull() ?: content
    }
}
