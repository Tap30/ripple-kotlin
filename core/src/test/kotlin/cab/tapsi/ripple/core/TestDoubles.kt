package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.HttpAdapter
import cab.tapsi.ripple.core.adapters.LoggerAdapter
import cab.tapsi.ripple.core.adapters.StorageAdapter
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.JsonObject

internal data class TestEvent(
    override val name: String = "test.event",
    private val payload: JsonObject? = null,
    override val schemaVersion: String? = null
) : RippleEvent {
    override fun getPayload(): JsonObject? = payload
}

internal data class TestMetadata(
    private val values: Map<String, Any>
) : RippleMetadata {
    override fun toMap(): Map<String, Any> = values
}

internal class TestRippleClient(
    config: RippleConfig,
    private val platform: Platform? = Platform.Server,
    private val anonymousIds: Iterator<String> = sequenceOf("anonymous-test").iterator(),
    private val eventIds: Iterator<String> = generateSequence(1) { it + 1 }
        .map { "event-$it" }
        .iterator()
) : RippleClient<TestEvent, TestMetadata>(config) {
    override fun getPlatform(): Platform? = platform
    override fun generateAnonymousId(): String = anonymousIds.next()
    override fun generateEventId(): String = eventIds.next()
    override fun getSdkInfo(): SdkInfo = SdkInfo("ripple-test", "1.0.0")
}

internal class RecordingHttpAdapter(
    private val responses: MutableList<Any> = mutableListOf(HttpResponse(ok = true, status = 200))
) : HttpAdapter {
    data class Request(
        val endpoint: String,
        val events: List<Event>,
        val headers: Map<String, String>,
        val apiKeyHeader: String
    )

    val requests = CopyOnWriteArrayList<Request>()

    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        requests += Request(endpoint, events.toList(), headers.toMap(), apiKeyHeader)
        val next = if (responses.isEmpty()) HttpResponse(ok = true, status = 200) else responses.removeAt(0)
        if (next is Exception) throw next
        return next as HttpResponse
    }
}

internal class BlockingHttpAdapter(
    private val response: HttpResponse = HttpResponse(ok = true, status = 200)
) : HttpAdapter {
    val requests = CopyOnWriteArrayList<List<Event>>()
    val entered = CountDownLatch(1)
    val release = CountDownLatch(1)

    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        requests += events.toList()
        entered.countDown()
        release.await(2, TimeUnit.SECONDS)
        return response
    }
}

internal class RecordingStorageAdapter(
    private val loadedEvents: List<Event> = emptyList(),
    private val throwOnLoad: Boolean = false,
    private val throwOnSave: Boolean = false
) : StorageAdapter {
    val saves = CopyOnWriteArrayList<List<Event>>()
    var initCount: Int = 0
        private set
    var clearCount: Int = 0
        private set

    override fun init() {
        initCount++
    }

    override fun save(events: List<Event>) {
        if (throwOnSave) error("save failed")
        saves += events.toList()
    }

    override fun load(): List<Event> {
        if (throwOnLoad) error("load failed")
        return loadedEvents
    }

    override fun clear() {
        clearCount++
    }
}

internal class RecordingLoggerAdapter : LoggerAdapter {
    data class Log(val level: String, val message: String, val args: List<Any?>)

    val logs = CopyOnWriteArrayList<Log>()

    override fun debug(message: String, vararg args: Any?) {
        logs += Log("debug", message, args.toList())
    }

    override fun info(message: String, vararg args: Any?) {
        logs += Log("info", message, args.toList())
    }

    override fun warn(message: String, vararg args: Any?) {
        logs += Log("warn", message, args.toList())
    }

    override fun error(message: String, vararg args: Any?) {
        logs += Log("error", message, args.toList())
    }
}

internal fun testConfig(
    http: RecordingHttpAdapter = RecordingHttpAdapter(),
    storage: RecordingStorageAdapter = RecordingStorageAdapter(),
    logger: RecordingLoggerAdapter = RecordingLoggerAdapter(),
    flushInterval: Long = 10_000L,
    maxBatchSize: Int = 10,
    maxRetries: Int = 1,
    batchOptions: BatchOptions = BatchOptions(),
    retryOptions: RetryOptions = RetryOptions(maxAttempts = maxRetries, minDelay = 1, maxDelay = 1, backoffFactor = 1.0),
    maxBufferSize: Int = 50,
    eventTtl: Long? = null,
    eventSampler: ((Event) -> Boolean)? = null,
    hooks: TelemetryHooks = TelemetryHooks(),
    telemetryOptions: TelemetryOptions? = null
): RippleConfig = RippleConfig(
    apiKey = "api-key",
    endpoint = "https://example.test/events",
    apiKeyHeader = "X-Test-Key",
    flushInterval = flushInterval,
    maxBatchSize = maxBatchSize,
    maxRetries = maxRetries,
    adapters = AdapterConfig(
        httpAdapter = http,
        storageAdapter = storage,
        loggerAdapter = logger
    ),
    batchOptions = batchOptions,
    retryOptions = retryOptions,
    maxBufferSize = maxBufferSize,
    eventTtl = eventTtl,
    eventSampler = eventSampler,
    hooks = hooks,
    telemetryOptions = telemetryOptions
)

internal fun event(
    name: String,
    issuedAt: Long = System.currentTimeMillis(),
    payload: JsonObject? = null
): Event = Event(
    name = name,
    payload = payload,
    issuedAt = issuedAt,
    metadata = null,
    platform = Platform.Server,
    sdk = SdkInfo("test", "1"),
    anonymousId = "anonymous",
    eventId = "event-$name"
)

internal fun eventually(timeoutMillis: Long = 1_000, assertion: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < deadline) {
        if (assertion()) return
        Thread.sleep(10)
    }
    check(assertion()) { "Condition was not met within ${timeoutMillis}ms" }
}
