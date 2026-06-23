package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.HttpAdapter
import cab.tapsi.ripple.core.adapters.StorageAdapter
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoreConcurrencyTest {
    @Test
    fun `concurrent track calls initialize once and preserve all events`() {
        val totalEvents = 200
        val http = RecordingHttpAdapter()
        val storage = RecordingStorageAdapter()
        val client = RippleClient.create(
            testConfig(
                http = http,
                storage = storage,
                maxBatchSize = totalEvents + 1,
                maxBufferSize = totalEvents + 1,
                flushInterval = 60_000L
            )
        )

        runConcurrently(workers = 8, iterations = 25) { worker, iteration ->
            client.track(
                name = "concurrent.$worker.$iteration",
                payload = mapOf("worker" to worker, "iteration" to iteration)
            )
        }

        eventually { client.getQueueSize() == totalEvents }
        assertEquals(1, storage.initCount)

        client.flush()

        eventually { http.requests.sumOf { it.events.size } == totalEvents }
        val sent = http.requests.flatMap { it.events }

        assertEquals(totalEvents, sent.map { it.name }.toSet().size)
        assertEquals(totalEvents, sent.map { it.eventId }.toSet().size)
        assertTrue(sent.all { it.anonymousId.isNotBlank() })

        client.dispose()
    }

    @Test
    fun `concurrent metadata writes and tracks keep complete metadata snapshot`() {
        val totalKeys = 120
        val client = RippleClient.create(
            testConfig(
                maxBatchSize = totalKeys + 1,
                maxBufferSize = totalKeys + 1,
                flushInterval = 60_000L
            )
        )

        runConcurrently(workers = 6, iterations = 20) { worker, iteration ->
            client.setMetadata("key-$worker-$iteration", "$worker-$iteration")
            client.getMetadata()?.forEach { (key, value) ->
                assertTrue(key.startsWith("key-"))
                assertTrue(value is String)
            }
            client.track("metadata.$worker.$iteration")
        }

        eventually { client.getQueueSize() == totalKeys }
        val metadata = client.getMetadata().orEmpty()

        assertEquals(totalKeys, metadata.size)
        assertEquals(
            (0 until 6).flatMap { worker -> (0 until 20).map { iteration -> "key-$worker-$iteration" } }.toSet(),
            metadata.keys
        )

        client.dispose()
    }

    @Test
    fun `concurrent dispatcher enqueue keeps queue consistent before flush`() {
        val totalEvents = 300
        val enqueues = CopyOnWriteArrayList<EnqueueInfo>()
        val dispatcher = dispatcher(
            maxBatchSize = totalEvents + 1,
            maxBufferSize = totalEvents + 1,
            flushInterval = 60_000L,
            hooks = TelemetryHooks(onEnqueue = { enqueues += it })
        )

        runConcurrently(workers = 10, iterations = 30) { worker, iteration ->
            dispatcher.enqueue(event("queued.$worker.$iteration"))
        }

        eventually { dispatcher.getQueueSize() == totalEvents }

        assertEquals(totalEvents, enqueues.size)
        assertEquals(totalEvents, enqueues.maxOf { it.bufferSize })

        dispatcher.dispose()
    }

    @Test
    fun `concurrent forced flushes send a queued batch once`() {
        val http = BlockingHttpAdapter()
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(
            http = http,
            logger = logger,
            maxBatchSize = 100,
            maxBufferSize = 100,
            flushInterval = 60_000L
        )

        repeat(20) { dispatcher.enqueue(event("flush-$it")) }

        runConcurrently(workers = 8, iterations = 1) { _, _ ->
            dispatcher.flush(force = true)
        }

        assertTrue(http.entered.await(1, TimeUnit.SECONDS))
        http.release.countDown()

        eventually { dispatcher.getQueueSize() == 0 }
        assertEquals(1, http.requests.size)
        assertEquals(20, http.requests.single().size)
        assertTrue(logger.logs.any { it.level == "debug" && it.message == "Flush already in progress, skipping" })

        dispatcher.dispose()
    }

    @Test
    fun `dispose racing with enqueue is idempotent and leaves dispatcher drained`() {
        val storage = RecordingStorageAdapter()
        val logger = RecordingLoggerAdapter()
        val dispatcher = dispatcher(
            storage = storage,
            logger = logger,
            maxBatchSize = 500,
            maxBufferSize = 500,
            flushInterval = 60_000L
        )

        runConcurrently(workers = 12, iterations = 25) { worker, iteration ->
            if (worker % 4 == 0 && iteration == 0) {
                dispatcher.dispose()
            } else {
                dispatcher.enqueue(event("race.$worker.$iteration"))
            }
        }

        dispatcher.dispose()

        assertEquals(0, dispatcher.getQueueSize())
        assertEquals(1, logger.logs.count { it.level == "debug" && it.message == "Disposing dispatcher" })
        assertTrue(storage.saves.all { saved -> saved.size <= 500 })
    }

    @Test
    fun `track racing with dispose does not throw or leak queued events`() {
        val storage = RecordingStorageAdapter()
        val logger = RecordingLoggerAdapter()
        val client = RippleClient.create(
            testConfig(
                storage = storage,
                logger = logger,
                maxBatchSize = 500,
                maxBufferSize = 500,
                flushInterval = 60_000L
            )
        )

        runConcurrently(workers = 10, iterations = 20) { worker, iteration ->
            if (worker == 0 && iteration % 5 == 0) {
                client.dispose()
            } else {
                client.track("client.race.$worker.$iteration")
            }
        }

        client.dispose()

        assertEquals(0, client.getQueueSize())
        assertTrue(logger.logs.any { it.level == "info" && it.message == "RippleClient disposed" })
        assertTrue(storage.saves.all { saved -> saved.size <= 500 })
    }

    private fun runConcurrently(
        workers: Int,
        iterations: Int,
        block: (worker: Int, iteration: Int) -> Unit
    ) {
        val executor = Executors.newFixedThreadPool(workers)
        val start = CountDownLatch(1)
        val done = CountDownLatch(workers)
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())

        repeat(workers) { worker ->
            executor.execute {
                try {
                    start.await(1, TimeUnit.SECONDS)
                    repeat(iterations) { iteration ->
                        block(worker, iteration)
                    }
                } catch (error: Throwable) {
                    failures += error
                } finally {
                    done.countDown()
                }
            }
        }

        start.countDown()
        assertTrue(done.await(5, TimeUnit.SECONDS), "Concurrent work did not finish in time")
        executor.shutdown()
        assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS), "Executor did not terminate")
        assertTrue(failures.isEmpty(), failures.joinToString(separator = "\n") { it.stackTraceToString() })
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
