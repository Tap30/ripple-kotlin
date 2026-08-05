package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.HttpAdapter
import cab.tapsi.ripple.core.adapters.LoggerAdapter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal class TelemetryReporter(
    private val endpoint: String,
    private val apiKey: String,
    private val apiKeyHeader: String,
    private val httpAdapter: HttpAdapter,
    private val loggerAdapter: LoggerAdapter,
    private val getAnonymousId: () -> String,
    private val getUserId: () -> String?,
    private val getMetadata: () -> Map<String, Any>?,
    private val getPlatform: () -> Platform?,
    private val getSdk: () -> SdkInfo,
    private val generateEventId: () -> String,
    flushInterval: Long = DEFAULT_FLUSH_INTERVAL,
    private val bufferCapacity: Int = DEFAULT_BUFFER_CAPACITY
) {
    private val buffer = ArrayDeque<Event>(bufferCapacity)
    private val bufferLock = Any()
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "ripple-telemetry").apply { isDaemon = true }
    }

    init {
        require(flushInterval > 0) { "Telemetry flush interval must be positive" }
        require(bufferCapacity > 0) { "Telemetry buffer capacity must be positive" }
        executor.scheduleAtFixedRate(::flush, flushInterval, flushInterval, TimeUnit.MILLISECONDS)
    }

    fun reportFlush(info: FlushInfo) {
        report(
            name = "sdk_event_flush",
            payload = buildJsonObject {
                put("eventCount", info.eventCount)
                put("batchCount", info.batchCount)
            }
        )
    }

    fun reportSendSuccess(info: SendSuccessInfo) {
        report(
            name = "sdk_event_send_success",
            payload = buildJsonObject {
                put("batchSize", info.batchSize)
                put("status", info.status)
            }
        )
    }

    fun reportSendFailure(info: SendFailureInfo) {
        report(
            name = "sdk_event_send_failure",
            payload = buildJsonObject {
                put("batchSize", info.batchSize)
                put("error", info.error)
                put("attempt", info.attempt)
            }
        )
    }

    fun reportRetry(info: RetryInfo) {
        report(
            name = "sdk_event_retry",
            payload = buildJsonObject {
                put("attempt", info.attempt)
                put("delay", info.delay)
            }
        )
    }

    fun reportDrop(info: DropInfo) {
        report(
            name = "sdk_event_drop",
            payload = buildJsonObject {
                put("eventCount", info.eventCount)
                put("reason", info.reason.name.lowercase())
            }
        )
    }

    private fun report(name: String, payload: JsonObject) {
        val event = Event(
            name = name,
            payload = payload,
            issuedAt = System.currentTimeMillis(),
            metadata = getMetadata(),
            platform = getPlatform(),
            sdk = getSdk(),
            anonymousId = getAnonymousId(),
            eventId = generateEventId(),
            schemaVersion = PREDEFINED_SCHEMA_VERSION,
            userId = getUserId()
        )

        synchronized(bufferLock) {
            if (buffer.size == bufferCapacity) buffer.removeFirst()
            buffer.addLast(event)
        }
    }

    fun dispose() {
        executor.shutdownNow()
        flush()
    }

    private fun flush() {
        val events = synchronized(bufferLock) {
            if (buffer.isEmpty()) return
            buffer.toList().also { buffer.clear() }
        }

        try {
            httpAdapter.send(
                endpoint = endpoint,
                events = events,
                headers = mapOf(
                    apiKeyHeader to apiKey,
                    "Content-Type" to "application/json"
                ),
                apiKeyHeader = apiKeyHeader
            )
        } catch (e: Exception) {
            loggerAdapter.debug(
                "Failed to report SDK telemetry",
                mapOf("eventCount" to events.size, "error" to (e.message ?: e::class.java.simpleName))
            )
        }
    }

    private companion object {
        const val DEFAULT_BUFFER_CAPACITY = 50
        const val DEFAULT_FLUSH_INTERVAL = 10_000L
    }
}
