package cab.tapsi.ripple.core

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.*

class RippleClientTest {
    @Test
    fun `track initializes client and sends typed event with metadata ids platform and sdk`() {
        val http = RecordingHttpAdapter()
        val storage = RecordingStorageAdapter()
        val client = TestRippleClient(
            config = testConfig(http = http, storage = storage, maxBatchSize = 1),
            platform = Platform.Server,
            anonymousIds = sequenceOf("anonymous-1").iterator(),
            eventIds = sequenceOf("event-1").iterator()
        )

        client.setMetadata(TestMetadata(mapOf("global" to "metadata")))
        client.track(
            TestEvent(
                name = "typed",
                payload = buildJsonObject { put("key", "value") },
                schemaVersion = "schema-1"
            )
        )

        eventually { http.requests.size == 1 }
        val sent = http.requests.single().events.single()

        assertEquals("typed", sent.name)
        assertEquals("value", sent.payload?.get("key")?.jsonPrimitive?.content)
        assertEquals(mapOf("global" to "metadata"), sent.metadata)
        assertEquals(Platform.Server, sent.platform)
        assertEquals(SdkInfo("ripple-test", "1.0.0"), sent.sdk)
        assertEquals(client.getAnonymousId(), sent.anonymousId)
        assertEquals("event-1", sent.eventId)
        assertEquals("schema-1", sent.schemaVersion)
        assertTrue(client.getAnonymousId().isNotBlank())
        assertEquals(1, storage.initCount)
    }

    @Test
    fun `untyped track converts map payload to json object`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 1))

        client.track("untyped", mapOf("nested" to mapOf("count" to 2)))

        eventually { http.requests.size == 1 }
        val payload = http.requests.single().events.single().payload!!

        assertEquals("untyped", http.requests.single().events.single().name)
        assertEquals("2", payload["nested"]?.jsonObject?.get("count")?.jsonPrimitive?.content)
    }

    @Test
    fun `untyped track overload accepts name only and null payload`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 1))

        client.track("name.only")

        eventually { http.requests.size == 1 }
        val sent = http.requests.single().events.single()

        assertEquals("name.only", sent.name)
        assertNull(sent.payload)
        assertNull(sent.schemaVersion)
    }

    @Test
    fun `companion create uses default ids sdk and null platform`() {
        val http = RecordingHttpAdapter()
        val client = RippleClient.create(testConfig(http = http, maxBatchSize = 1))

        client.track(DefaultRippleEvent("created", buildJsonObject { put("source", "factory") }))

        eventually { http.requests.size == 1 }
        val sent = http.requests.single().events.single()

        assertEquals("created", sent.name)
        assertEquals("factory", sent.payload?.get("source")?.jsonPrimitive?.content)
        assertNull(sent.platform)
        assertEquals(SdkInfo("ripple-kotlin", "unknown"), sent.sdk)
        assertTrue(sent.anonymousId.isNotBlank())
        assertTrue(sent.eventId.isNotBlank())
    }

    @Test
    fun `companion created client can dispose and reinitialize with anonymous id`() {
        val client = RippleClient.create(testConfig())

        client.init()
        val firstAnonymousId = client.getAnonymousId()
        client.dispose()
        client.init()

        assertTrue(firstAnonymousId.isNotBlank())
        assertTrue(client.getAnonymousId().isNotBlank())

        client.dispose()
    }

    @Test
    fun `identify stores user id and emits predefined event`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 1))

        client.identify("user-1", UserTraits(email = "u@example.test"))

        eventually { http.requests.size == 1 }
        val sent = http.requests.single().events.single()

        assertEquals("user-1", client.getUserId())
        assertEquals("user_identified", sent.name)
        assertEquals("user-1", sent.userId)
        assertEquals("user-1", sent.payload?.get("userId")?.jsonPrimitive?.content)
        assertEquals("u@example.test", sent.payload?.get("traits")?.jsonObject?.get("email")?.jsonPrimitive?.content)
    }

    @Test
    fun `predefined convenience methods emit expected event names`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 1))

        client.clicked(ClickedPayload(elementId = "button"))
        client.viewed(ViewedPayload(elementId = "card"))
        client.screen(ScreenPayload(title = "Home"))
        client.appOpened()
        client.appClosed()

        eventually { http.requests.size == 5 }
        assertEquals(
            listOf("clicked", "viewed", "screened", "app_state_changed", "app_state_changed"),
            http.requests.flatMap { it.events }.map { it.name }
        )
    }

    @Test
    fun `events namespace emits predefined event through client`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 1))

        client.events.productClicked(
            ProductPayload(Product(productId = "p1", price = Money(amount = 10, currency = "IRR")))
        )

        eventually { http.requests.size == 1 }
        assertEquals("product_clicked", http.requests.single().events.single().name)
    }

    @Test
    fun `automatic telemetry reports dispatcher hook events to telemetry endpoint`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(
            testConfig(
                http = http,
                maxBatchSize = 10,
                telemetryOptions = TelemetryOptions(endpoint = "https://telemetry.example.test/events")
            )
        )

        client.track("telemetry.test")

        eventually { http.requests.any { it.endpoint == "https://telemetry.example.test/events" } }
        val telemetryEvent = http.requests
            .first { it.endpoint == "https://telemetry.example.test/events" }
            .events
            .single()

        assertEquals("sdk_event_enqueue", telemetryEvent.name)
        assertEquals(PREDEFINED_SCHEMA_VERSION, telemetryEvent.schemaVersion)
        assertEquals(client.getAnonymousId(), telemetryEvent.anonymousId)
    }

    @Test
    fun `automatic telemetry respects disabled option`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(
            testConfig(
                http = http,
                maxBatchSize = 1,
                telemetryOptions = TelemetryOptions(
                    endpoint = "https://telemetry.example.test/events",
                    disabled = true
                )
            )
        )

        client.track("telemetry.disabled")

        eventually { http.requests.any { it.endpoint == "https://example.test/events" } }
        assertTrue(http.requests.none { it.endpoint == "https://telemetry.example.test/events" })
    }

    @Test
    fun `automatic telemetry preserves user hooks`() {
        val http = RecordingHttpAdapter()
        val enqueues = mutableListOf<EnqueueInfo>()
        val client = TestRippleClient(
            testConfig(
                http = http,
                maxBatchSize = 10,
                hooks = TelemetryHooks(onEnqueue = { enqueues += it }),
                telemetryOptions = TelemetryOptions(endpoint = "https://telemetry.example.test/events")
            )
        )

        client.track("telemetry.hook")

        eventually {
            enqueues.isNotEmpty() &&
                http.requests.any { it.endpoint == "https://telemetry.example.test/events" }
        }

        assertEquals(EnqueueInfo(bufferSize = 1), enqueues.single())
    }

    @Test
    fun `metadata can be read and cleared`() {
        val client = TestRippleClient(testConfig())

        assertNull(client.getMetadata())
        client.setMetadata("key", "value")
        client.setMetadata(TestMetadata(mapOf("other" to 2)))

        assertEquals(mapOf("key" to "value", "other" to 2), client.getMetadata())

        client.clearMetadata()

        assertNull(client.getMetadata())
    }

    @Test
    fun `flush before initialization is ignored and queue size starts at zero`() {
        val client = TestRippleClient(testConfig())

        client.flush()

        assertEquals(0, client.getQueueSize())
    }

    @Test
    fun `flush after initialization sends queued event`() {
        val http = RecordingHttpAdapter()
        val client = TestRippleClient(testConfig(http = http, maxBatchSize = 10))

        client.track("queued", mapOf("x" to 1))
        eventually { client.getQueueSize() == 1 }
        client.flush()

        eventually { http.requests.size == 1 }
        assertEquals("queued", http.requests.single().events.single().name)
        assertEquals(0, client.getQueueSize())
    }

    @Test
    fun `track after dispose is ignored and logged`() {
        val http = RecordingHttpAdapter()
        val logger = RecordingLoggerAdapter()
        val client = TestRippleClient(testConfig(http = http, logger = logger, maxBatchSize = 1))

        client.dispose()
        client.track("ignored", mapOf("x" to 1))

        assertEquals(0, http.requests.size)
        assertTrue(logger.logs.any { it.level == "warn" && it.message == "Cannot track event: Client has been disposed" })
    }

    @Test
    fun `dispose keeps identity state and persists queued events`() {
        val storage = RecordingStorageAdapter()
        val client = TestRippleClient(testConfig(storage = storage, maxBatchSize = 10))

        client.setMetadata("key", "value")
        client.track("queued", mapOf("x" to 1))
        eventually { storage.saves.isNotEmpty() }
        client.identify("user-1")
        client.dispose()

        assertTrue(client.getAnonymousId().isNotBlank())
        assertEquals("user-1", client.getUserId())
        assertNull(client.getMetadata())
        assertTrue(storage.saves.any { saved -> saved.any { it.name == "queued" } })
    }

    @Test
    fun `explicit init supports reinitialization after dispose`() {
        val client = TestRippleClient(
            config = testConfig(),
            anonymousIds = sequenceOf("anonymous-1", "anonymous-2").iterator()
        )

        client.init()
        val firstAnonymousId = client.getAnonymousId()
        client.dispose()
        client.init()

        assertEquals("anonymous-1", firstAnonymousId)
        assertEquals("anonymous-1", client.getAnonymousId())
    }

    @Test
    fun `id generator returns unique nonblank ids`() {
        val first = IdGenerator.generate()
        val second = IdGenerator.generate()

        assertNotNull(first)
        assertTrue(first.isNotBlank())
        assertNotEquals(first, second)
    }
}
