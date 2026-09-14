package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.HttpAdapter
import cab.tapsi.ripple.core.adapters.LoggerAdapter
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    private val generateEventId: () -> String
) {
    fun reportFlush(info: FlushInfo) {
        report(
            name = "sdk_event_flush",
            payload = buildJsonObject {
                put("eventCount", info.eventCount.toDouble())
                put("batchCount", info.batchCount.toDouble())
            }
        )
    }

    fun reportSendSuccess(info: SendSuccessInfo) {
        report(
            name = "sdk_event_send_success",
            payload = buildJsonObject {
                put("batchSize", info.batchSize.toDouble())
                put("status", info.status.toDouble())
            }
        )
    }

    fun reportSendFailure(info: SendFailureInfo) {
        report(
            name = "sdk_event_send_failure",
            payload = buildJsonObject {
                put("batchSize", info.batchSize.toDouble())
                put("error", info.error)
                put("attempt", info.attempt.toDouble())
            }
        )
    }

    fun reportRetry(info: RetryInfo) {
        report(
            name = "sdk_event_retry",
            payload = buildJsonObject {
                put("attempt", info.attempt.toDouble())
                put("delay", info.delay.toDouble())
            }
        )
    }

    fun reportDrop(info: DropInfo) {
        report(
            name = "sdk_event_drop",
            payload = buildJsonObject {
                put("eventCount", info.eventCount.toDouble())
                put(
                    "reason",
                    if (info.reason == DropReason.BUFFER_FULL) "client_error" else info.reason.name.lowercase()
                )
            }
        )
    }

    fun reportEnqueue(info: EnqueueInfo) {
        report(
            name = "sdk_event_enqueue",
            payload = buildJsonObject {
                put("bufferSize", info.bufferSize.toDouble())
            }
        )
    }

    private fun report(name: String, payload: JsonObject) {
        try {
            httpAdapter.send(
                endpoint = endpoint,
                events = listOf(
                    Event(
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
                ),
                headers = mapOf(
                    apiKeyHeader to apiKey,
                    "Content-Type" to "application/json"
                ),
                apiKeyHeader = apiKeyHeader
            )
        } catch (e: Exception) {
            loggerAdapter.debug(
                "Failed to report SDK telemetry",
                mapOf("event" to name, "error" to (e.message ?: e::class.java.simpleName))
            )
        }
    }
}
