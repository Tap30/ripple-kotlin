package cab.tapsi.ripple.core

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class JsonPayloadMapperTest {
    @Test
    fun `map payload converts supported primitive and nested values to json`() {
        val payload = mapOf(
            "string" to "value",
            "int" to 3,
            "long" to 4L,
            "double" to 2.5,
            "boolean" to true,
            "map" to mapOf("nested" to "yes"),
            "list" to listOf(1, "two", false),
            "array" to arrayOf("a", 1),
            "json" to JsonObject(mapOf("kept" to JsonPrimitive("as-is")))
        ).toJsonObject()

        assertEquals("value", payload["string"]?.jsonPrimitive?.content)
        assertEquals(3, payload["int"]?.jsonPrimitive?.int)
        assertEquals(4L, payload["long"]?.jsonPrimitive?.content?.toLong())
        assertEquals(2.5, payload["double"]?.jsonPrimitive?.double)
        assertEquals(true, payload["boolean"]?.jsonPrimitive?.boolean)
        assertEquals("yes", payload["map"]?.jsonObject?.get("nested")?.jsonPrimitive?.content)
        assertEquals(3, payload["list"]?.jsonArray?.size)
        assertEquals("a", payload["array"]?.jsonArray?.get(0)?.jsonPrimitive?.content)
        assertEquals("as-is", payload["json"]?.jsonObject?.get("kept")?.jsonPrimitive?.content)
    }

    @Test
    fun `map payload stringifies unknown value types`() {
        val payload = mapOf("custom" to StringBuilder("builder")).toJsonObject()

        assertEquals("builder", payload["custom"]?.jsonPrimitive?.content)
    }

    @Test
    fun `json object converts back to payload map and removes nulls`() {
        val payload = JsonObject(
            mapOf(
                "string" to JsonPrimitive("value"),
                "long" to JsonPrimitive(42),
                "double" to JsonPrimitive(1.5),
                "boolean" to JsonPrimitive(false),
                "null" to JsonNull,
                "nested" to JsonObject(mapOf("inside" to JsonPrimitive("ok"))),
                "array" to kotlinx.serialization.json.JsonArray(
                    listOf(JsonPrimitive("kept"), JsonNull, JsonPrimitive(2))
                )
            )
        ).toPayloadMap()

        assertEquals("value", payload["string"])
        assertEquals(42L, payload["long"])
        assertEquals(1.5, payload["double"])
        assertEquals(false, payload["boolean"])
        assertFalse(payload.containsKey("null"))
        assertEquals(mapOf("inside" to "ok"), payload["nested"])
        assertEquals(listOf("kept", 2L), payload["array"])
    }

    @Test
    fun `default event returns provided json payload`() {
        val payload = mapOf("key" to "value").toJsonObject()
        val event = DefaultRippleEvent(name = "default", payloadData = payload)

        assertEquals("default", event.name)
        assertEquals(payload, event.getPayload())
    }

    @Test
    fun `default metadata returns provided map`() {
        val metadata = DefaultRippleMetadata(mapOf("user" to "u1"))

        assertEquals(mapOf("user" to "u1"), metadata.toMap())
    }
}
