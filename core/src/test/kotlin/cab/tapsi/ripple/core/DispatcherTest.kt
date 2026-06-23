package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.HttpAdapter
import cab.tapsi.ripple.core.adapters.StorageAdapter
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DispatcherTest {
    @Test
    fun `dispatcher config rejects impossible buffer and batch sizes`() {
        assertFailsWith<IllegalArgumentException> {
            dispatcher(maxBatchSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            dispatcher(maxPayloadSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            dispatcher(maxBufferSize = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            dispatcher(maxBatchSize = 2, maxBufferSize = 1)
        }
    }

    @Test
    fun `dispatcher config defaults are usable`() {
        val config = Dispatcher.DispatcherConfig(
            endpoint = "https://example.test/events",
            apiKey = "api-key",
            apiKeyHeader = "X-Test-Key",
            flushInterval = 10_000L,
            maxBatchSize = 10,
            maxRetries = 1
        )

        assertEquals(64L * 1024L, config.maxPayloadSize)
        assertEquals(50, config.maxBufferSize)
        assertEquals(null, config.eventTtl)
        assertEquals(RetryOptions(), config.retryOptions)
        assertEquals(null, config.eventSampler)
        assertEquals(TelemetryHooks(), config.hooks)
    }

    @Test
    fun `force flush on empty queue and flush after dispose are noops`() {
        val http = RecordingHttpAdapter()
        val dispatcher = dispatcher(http = http, maxBatchSize = 1)

        dispatcher.flush(force = true)
        eventually { dispatcher.getQueueSize() == 0 }
        dispatcher.dispose()
        dispatcher.flush(force = true)

        assertEquals(0, http.requests.size)
    }

    @Test
    fun `enqueue below batch size persists snapshot without sending immediately`() {
        val http = RecordingHttpAdapter()
        val storage = RecordingStorageAdapter()
        val dispatcher = dispatcher(http = http, storage = storage, maxBatchSize = 2)

        dispatcher.enqueue(event("one"))

        eventually { storage.saves.isNotEmpty() }
        assertEquals(1, dispatcher.getQueueSize())
        assertEquals(emptyList(), http.requests.toList())

        dispatcher.dispose()
    }

    @Test
    fun `batch threshold sends events with headers and success hook`() {
        val http = RecordingHttpAdapter()
        val success = mutableListOf<SendSuccessInfo>()
        val flushes = mutableListOf<FlushInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 2,
            hooks = TelemetryHooks(
                onSendSuccess = { success += it },
                onFlush = { flushes += it }
            )
        )

        dispatcher.enqueue(event("one"))
        dispatcher.enqueue(event("two"))

        eventually { http.requests.size == 1 }
        val request = http.requests.single()

        assertEquals(listOf("one", "two"), request.events.map { it.name })
        assertEquals("api-key", request.headers["X-Test-Key"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("X-Test-Key", request.apiKeyHeader)
        assertEquals(SendSuccessInfo(batchSize = 2, status = 200), success.single())
        assertEquals(FlushInfo(eventCount = 2, batchCount = 1), flushes.single())
        assertEquals(0, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `force flush sends partial batch`() {
        val http = RecordingHttpAdapter()
        val dispatcher = dispatcher(http = http, maxBatchSize = 10)

        dispatcher.enqueue(event("one"))
        dispatcher.flush(force = true)

        eventually { http.requests.size == 1 }
        assertEquals(listOf("one"), http.requests.single().events.map { it.name })
        assertEquals(0, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `scheduled flush task runs and later threshold flush sends queued events`() {
        val http = RecordingHttpAdapter()
        val dispatcher = dispatcher(http = http, maxBatchSize = 2, flushInterval = 20L)

        dispatcher.enqueue(event("one"))
        Thread.sleep(60)
        dispatcher.enqueue(event("two"))

        eventually { http.requests.size == 1 }
        assertEquals(listOf("one"), http.requests.single().events.map { it.name })

        dispatcher.dispose()
    }

    @Test
    fun `flush while another flush is in progress is skipped and logged`() {
        val http = BlockingHttpAdapter()
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(http = http, logger = logger, maxBatchSize = 1)

        dispatcher.enqueue(event("one"))
        assertTrue(http.entered.await(1, TimeUnit.SECONDS))

        dispatcher.flush(force = true)

        eventually { logger.logs.any { it.level == "debug" && it.message == "Flush already in progress, skipping" } }
        http.release.countDown()
        eventually { dispatcher.getQueueSize() == 0 }

        dispatcher.dispose()
    }

    @Test
    fun `client error drops batch without retry`() {
        val http = RecordingHttpAdapter(mutableListOf(HttpResponse(ok = false, status = 422)))
        val drops = mutableListOf<DropInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            hooks = TelemetryHooks(onDrop = { drops += it })
        )

        dispatcher.enqueue(event("bad"))

        eventually { drops.isNotEmpty() }
        assertEquals(1, http.requests.size)
        assertEquals(DropInfo(eventCount = 1, reason = DropReason.CLIENT_ERROR), drops.single())
        assertEquals(0, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `unexpected non success status drops batch and persists empty queue`() {
        val http = RecordingHttpAdapter(mutableListOf(HttpResponse(ok = false, status = 302, data = "redirect")))
        val storage = RecordingStorageAdapter()
        val dispatcher = dispatcher(http = http, storage = storage, maxBatchSize = 1)

        dispatcher.enqueue(event("redirect"))

        eventually { http.requests.size == 1 && dispatcher.getQueueSize() == 0 }
        eventually { storage.saves.any { it.isEmpty() } }
        assertTrue(storage.saves.any { it.isEmpty() })

        dispatcher.dispose()
    }

    @Test
    fun `server error retries then succeeds`() {
        val http = RecordingHttpAdapter(
            mutableListOf(
                HttpResponse(ok = false, status = 500),
                HttpResponse(ok = true, status = 204)
            )
        )
        val retries = mutableListOf<RetryInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            maxRetries = 2,
            hooks = TelemetryHooks(onRetry = { retries += it })
        )

        dispatcher.enqueue(event("retry"))

        eventually { http.requests.size == 2 }
        assertEquals(1, retries.size)
        assertEquals(0, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `network error retries then succeeds`() {
        val http = RecordingHttpAdapter(
            mutableListOf(
                IllegalStateException("temporary"),
                HttpResponse(ok = true, status = 200)
            )
        )
        val retries = mutableListOf<RetryInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            maxRetries = 2,
            hooks = TelemetryHooks(onRetry = { retries += it })
        )

        dispatcher.enqueue(event("retry-network"))

        eventually { http.requests.size == 2 }
        assertEquals(1, retries.size)
        assertEquals(0, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `server error at max retries requeues and persists failed batch in fifo order`() {
        val http = RecordingHttpAdapter(mutableListOf(HttpResponse(ok = false, status = 503)))
        val storage = RecordingStorageAdapter()
        val failures = mutableListOf<SendFailureInfo>()
        val dispatcher = dispatcher(
            http = http,
            storage = storage,
            maxBatchSize = 2,
            maxRetries = 1,
            hooks = TelemetryHooks(onSendFailure = { failures += it })
        )

        dispatcher.enqueue(event("one"))
        dispatcher.enqueue(event("two"))

        eventually { failures.isNotEmpty() }
        assertEquals(listOf("one", "two"), storage.saves.last().map { it.name })
        assertEquals(2, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `payload size boundary requeues sent batch plus current event on failure`() {
        val http = RecordingHttpAdapter(mutableListOf(HttpResponse(ok = false, status = 503)))
        val storage = RecordingStorageAdapter()
        val failures = mutableListOf<SendFailureInfo>()
        val dispatcher = dispatcher(
            http = http,
            storage = storage,
            maxBatchSize = 10,
            maxPayloadSize = 1,
            maxRetries = 1,
            hooks = TelemetryHooks(onSendFailure = { failures += it })
        )

        dispatcher.enqueue(event("one", payload = mapOf("value" to "large").toJsonObject()))
        dispatcher.enqueue(event("two", payload = mapOf("value" to "large").toJsonObject()))
        dispatcher.flush(force = true)

        eventually { failures.isNotEmpty() }
        assertEquals(listOf("one", "two"), storage.saves.last().map { it.name })
        assertEquals(2, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `network error at max retries requeues and reports failure`() {
        val http = RecordingHttpAdapter(mutableListOf(IllegalStateException("offline")))
        val failures = mutableListOf<SendFailureInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            maxRetries = 1,
            hooks = TelemetryHooks(onSendFailure = { failures += it })
        )

        dispatcher.enqueue(event("offline"))

        eventually { failures.isNotEmpty() }
        assertEquals("offline", failures.single().error)
        assertEquals(1, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `network error without message reports exception class name`() {
        val http = RecordingHttpAdapter(mutableListOf(Exception()))
        val failures = mutableListOf<SendFailureInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            maxRetries = 1,
            hooks = TelemetryHooks(onSendFailure = { failures += it })
        )

        dispatcher.enqueue(event("offline"))

        eventually { failures.isNotEmpty() }
        assertEquals("Exception", failures.single().error)

        dispatcher.dispose()
    }

    @Test
    fun `interrupted retry sleep requeues without retrying again`() {
        val http = object : HttpAdapter {
            val requests = mutableListOf<List<Event>>()

            override fun send(
                endpoint: String,
                events: List<Event>,
                headers: Map<String, String>,
                apiKeyHeader: String
            ): HttpResponse {
                requests += events.toList()
                Thread.currentThread().interrupt()
                return HttpResponse(ok = false, status = 500)
            }
        }
        val dispatcher = dispatcher(http = http, maxBatchSize = 1, maxRetries = 2)

        dispatcher.enqueue(event("interrupted"))

        eventually { dispatcher.getQueueSize() == 1 }

        dispatcher.dispose()
    }

    @Test
    fun `event sampler drops sampled out events`() {
        val http = RecordingHttpAdapter()
        val drops = mutableListOf<DropInfo>()
        val dispatcher = dispatcher(
            http = http,
            eventSampler = { false },
            hooks = TelemetryHooks(onDrop = { drops += it })
        )

        dispatcher.enqueue(event("sampled"))

        assertEquals(0, dispatcher.getQueueSize())
        assertEquals(0, http.requests.size)
        assertEquals(DropInfo(eventCount = 1, reason = DropReason.SAMPLED), drops.single())

        dispatcher.dispose()
    }

    @Test
    fun `enqueue hook receives buffer size`() {
        val enqueues = mutableListOf<EnqueueInfo>()
        val dispatcher = dispatcher(
            maxBatchSize = 10,
            hooks = TelemetryHooks(onEnqueue = { enqueues += it })
        )

        dispatcher.enqueue(event("one"))
        dispatcher.enqueue(event("two"))

        assertEquals(listOf(EnqueueInfo(1), EnqueueInfo(2)), enqueues)

        dispatcher.dispose()
    }

    @Test
    fun `expired events are dropped before send`() {
        val http = RecordingHttpAdapter()
        val drops = mutableListOf<DropInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            eventTtl = 1L,
            hooks = TelemetryHooks(onDrop = { drops += it })
        )

        dispatcher.enqueue(event("old", issuedAt = System.currentTimeMillis() - 10_000))
        dispatcher.flush(force = true)

        eventually { drops.isNotEmpty() }
        assertEquals(DropInfo(eventCount = 1, reason = DropReason.EXPIRED), drops.single())
        assertEquals(0, http.requests.size)
        assertEquals(0, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `buffer full drops oldest event`() {
        val drops = mutableListOf<DropInfo>()
        val http = BlockingHttpAdapter()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 3,
            maxBufferSize = 3,
            hooks = TelemetryHooks(onDrop = { drops += it })
        )

        dispatcher.enqueue(event("one"))
        dispatcher.enqueue(event("two"))
        dispatcher.enqueue(event("three"))
        assertTrue(http.entered.await(1, TimeUnit.SECONDS))
        dispatcher.enqueue(event("four"))
        dispatcher.enqueue(event("five"))
        dispatcher.enqueue(event("six"))
        dispatcher.enqueue(event("seven"))

        eventually { drops.isNotEmpty() }
        assertEquals(DropInfo(eventCount = 1, reason = DropReason.BUFFER_FULL), drops.single())
        assertTrue(dispatcher.getQueueSize() <= 3)

        http.release.countDown()
        dispatcher.dispose()
    }

    @Test
    fun `failed requeue trims existing buffer when buffer is full`() {
        val http = BlockingHttpAdapter(HttpResponse(ok = false, status = 503))
        val drops = mutableListOf<DropInfo>()
        val dispatcher = dispatcher(
            http = http,
            maxBatchSize = 1,
            maxBufferSize = 1,
            maxRetries = 1,
            hooks = TelemetryHooks(onDrop = { drops += it })
        )

        dispatcher.enqueue(event("one"))
        assertTrue(http.entered.await(1, TimeUnit.SECONDS))
        dispatcher.enqueue(event("two"))
        http.release.countDown()

        eventually { drops.any { it.reason == DropReason.BUFFER_FULL } }
        assertEquals(1, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `restore loads persisted events and caps to buffer size`() {
        val storage = RecordingStorageAdapter(
            loadedEvents = listOf(event("one"), event("two"), event("three"))
        )
        val dispatcher = dispatcher(storage = storage, maxBatchSize = 2, maxBufferSize = 2)

        dispatcher.restore()

        assertEquals(2, dispatcher.getQueueSize())

        dispatcher.dispose()
    }

    @Test
    fun `restore logs and keeps empty queue when storage load fails`() {
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(
            storage = RecordingStorageAdapter(throwOnLoad = true),
            logger = logger
        )

        dispatcher.restore()

        assertEquals(0, dispatcher.getQueueSize())
        assertTrue(logger.logs.any { it.level == "error" && it.message == "Failed to restore events from storage" })

        dispatcher.dispose()
    }

    @Test
    fun `dispose persists queued events and ignores later enqueue`() {
        val storage = RecordingStorageAdapter()
        val dispatcher = dispatcher(storage = storage, maxBatchSize = 10)

        dispatcher.enqueue(event("queued"))
        eventually { storage.saves.isNotEmpty() }
        dispatcher.dispose()
        dispatcher.enqueue(event("ignored"))

        assertTrue(storage.saves.any { it.map(Event::name) == listOf("queued") })
        assertEquals(0, dispatcher.getQueueSize())
    }

    @Test
    fun `dispose is idempotent`() {
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(logger = logger, maxBatchSize = 10)

        dispatcher.enqueue(event("queued"))
        dispatcher.dispose()
        dispatcher.dispose()

        assertEquals(1, logger.logs.count { it.level == "debug" && it.message == "Disposing dispatcher" })
    }

    @Test
    fun `dispose logs storage save failure`() {
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(
            storage = RecordingStorageAdapter(throwOnSave = true),
            logger = logger,
            maxBatchSize = 10
        )

        dispatcher.enqueue(event("queued"))
        dispatcher.dispose()

        assertTrue(logger.logs.any { it.level == "error" && it.message.startsWith("Failed to persist events on dispose") })
    }

    @Test
    fun `async buffer persistence logs storage save failure`() {
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(
            storage = RecordingStorageAdapter(throwOnSave = true),
            logger = logger,
            maxBatchSize = 10
        )

        dispatcher.enqueue(event("queued"))

        eventually {
            logger.logs.any {
                it.level == "error" && it.message == "Failed to persist events to storage"
            }
        }

        dispatcher.dispose()
    }

    @Test
    fun `storage save failure without message logs exception class name`() {
        val logger = RecordingLoggerAdapter()
        val storage = object : StorageAdapter {
            override fun save(events: List<Event>) {
                throw Exception()
            }

            override fun load(): List<Event> = emptyList()

            override fun clear() = Unit
        }
        val dispatcher = dispatcher(
            storage = storage,
            logger = logger,
            maxBatchSize = 10
        )

        dispatcher.enqueue(event("queued"))

        eventually {
            logger.logs.any {
                it.level == "error" &&
                    it.message == "Failed to persist events to storage" &&
                    it.args.toString().contains("Exception")
            }
        }

        dispatcher.dispose()
    }

    private fun dispatcher(
        http: HttpAdapter = RecordingHttpAdapter(),
        storage: StorageAdapter = RecordingStorageAdapter(),
        logger: RecordingLoggerAdapter = RecordingLoggerAdapter(),
        flushInterval: Long = 10_000L,
        maxBatchSize: Int = 10,
        maxRetries: Int = 1,
        maxPayloadSize: Long = 65_536L,
        maxBufferSize: Int = 50,
        eventTtl: Long? = null,
        eventSampler: ((Event) -> Boolean)? = null,
        hooks: TelemetryHooks = TelemetryHooks()
    ): Dispatcher = Dispatcher(
        config = Dispatcher.DispatcherConfig(
            endpoint = "https://example.test/events",
            apiKey = "api-key",
            apiKeyHeader = "X-Test-Key",
            flushInterval = flushInterval,
            maxBatchSize = maxBatchSize,
            maxRetries = maxRetries,
            maxPayloadSize = maxPayloadSize,
            maxBufferSize = maxBufferSize,
            eventTtl = eventTtl,
            retryOptions = RetryOptions(maxAttempts = maxRetries, minDelay = 1, maxDelay = 1, backoffFactor = 1.0),
            eventSampler = eventSampler,
            hooks = hooks
        ),
        httpAdapter = http,
        storageAdapter = storage,
        loggerAdapter = logger
    )
}
