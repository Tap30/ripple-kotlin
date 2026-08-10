package cab.tapsi.ripple.core

import cab.tapsi.ripple.core.adapters.HttpAdapter
import cab.tapsi.ripple.core.adapters.LoggerAdapter
import cab.tapsi.ripple.core.adapters.StorageAdapter
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.pow
import kotlin.random.Random

/**
 * Event dispatcher handling queue management, batching, and retry logic.
 *
 * The structure mirrors the TypeScript dispatcher:
 * - enqueue adds to the in-memory buffer and persists a best-effort snapshot asynchronously
 * - flush drains the current buffer, filters expired events, then rebatches dynamically
 * - response, retry, post-send persistence, and requeue handling live in focused helpers
 */
class Dispatcher(
    private val config: DispatcherConfig,
    private val httpAdapter: HttpAdapter,
    private val storageAdapter: StorageAdapter,
    private val loggerAdapter: LoggerAdapter
) {
    private val eventQueue = ArrayDeque<Event>()
    private val queueLock = ReentrantLock()
    private val storageLock = ReentrantLock()

    private val isFlushInProgress = AtomicBoolean(false)
    private val isDisposed = AtomicBoolean(false)

    private val scheduleLock = Any()
    private var scheduledFlushTask: ScheduledFuture<*>? = null

    private val executor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { runnable ->
            Thread(runnable, "ripple-dispatcher").apply { isDaemon = true }
        }

    private val json = Json {
        encodeDefaults = true
        classDiscriminator = "platformType"
    }



    data class DispatcherConfig(
        val endpoint: String,
        val apiKey: String,
        val apiKeyHeader: String,
        val flushInterval: Long,
        val maxBatchSize: Int,
        val maxRetries: Int,
        val maxPayloadSize: Long = DEFAULT_MAX_PAYLOAD_SIZE,
        val maxBufferSize: Int = DEFAULT_MAX_BUFFER_SIZE,
        val eventTtl: Long? = null,
        val retryOptions: RetryOptions = RetryOptions(),
        val eventSampler: ((Event) -> Boolean)? = null,
        val hooks: TelemetryHooks = TelemetryHooks()
    )

    init {
        require(config.maxBatchSize > 0) { "maxBatchSize must be greater than 0" }
        require(config.maxPayloadSize > 0) { "maxPayloadSize must be greater than 0" }
        require(config.maxBufferSize > 0) { "maxBufferSize must be greater than 0" }
        require(config.maxBufferSize >= config.maxBatchSize) {
            "Invalid configuration: maxBufferSize (${config.maxBufferSize}) must be >= maxBatchSize (${config.maxBatchSize}). " +
                "The batch size will never be reached and events will be dropped unnecessarily."
        }
    }

    /**
     * Add an event to the queue. Triggers auto-flush if the batch threshold is reached.
     */
    fun enqueue(event: Event) {
        if (isDisposed.get()) {
            loggerAdapter.warn("Cannot enqueue event: dispatcher is disposed")
            return
        }

        if (config.eventSampler?.invoke(event) == false) {
            return
        }

        val size = offerEvent(event)

        config.hooks.onEnqueue?.invoke(EnqueueInfo(bufferSize = size))
        loggerAdapter.debug("Event enqueued: ${event.name}, queue size: $size")

        persistBufferAsync()

        flush()
    }

    /**
     * Immediately flush queued events in the background.
     */
    fun flush(force: Boolean = false) {
        if (isDisposed.get()) return

        if (!force && getQueueSize() < config.maxBatchSize) {
            scheduleFlush()
            return
        }

        if (!isFlushInProgress.compareAndSet(false, true)) {
            loggerAdapter.debug("Flush already in progress, skipping")
            return
        }

        executor.execute {
            try {
                flushInternal()
            } finally {
                isFlushInProgress.set(false)
            }
        }
    }

    private fun flushInternal() {
        cancelScheduledFlush()

        if (isQueueEmpty()) return

        val ttl = config.eventTtl
        val now = if (ttl != null) System.currentTimeMillis() else 0L

        var totalEvents = 0
        var totalBatches = 0
        var droppedCount = 0
        var batch = mutableListOf<Event>()
        var batchPayloadSize = 0L

        while (true) {
            val event = pollEvent() ?: break

            if (ttl != null && now - event.issuedAt > ttl) {
                droppedCount++
                continue
            }

            val eventSize = serializedSize(event)

            if (
                batch.isNotEmpty() &&
                (batch.size >= config.maxBatchSize ||
                    batchPayloadSize + eventSize > config.maxPayloadSize)
            ) {
                totalEvents += batch.size
                totalBatches++

                if (!sendWithRetry(batch)) {
                    requeueBatch(batch + event)
                    reportDroppedExpired(droppedCount)
                    return
                }

                batch = mutableListOf()
                batchPayloadSize = 0L
            }

            batch.add(event)
            batchPayloadSize += eventSize
        }

        reportDroppedExpired(droppedCount)

        if (batch.isNotEmpty()) {
            totalEvents += batch.size
            totalBatches++

            config.hooks.onFlush?.invoke(
                FlushInfo(eventCount = totalEvents, batchCount = totalBatches)
            )

            if (!sendWithRetry(batch)) {
                requeueBatch(batch)
                return
            }
        }
    }

    /**
     * Send events with exponential backoff retry logic.
     */
    private fun sendWithRetry(events: List<Event>, attempt: Int = 0): Boolean {
        return try {
            val response = httpAdapter.send(
                endpoint = config.endpoint,
                events = events,
                headers = mapOf(
                    config.apiKeyHeader to config.apiKey,
                    "Content-Type" to "application/json"
                ),
                apiKeyHeader = config.apiKeyHeader
            )

            handleResponse(response, events, attempt)
        } catch (e: Exception) {
            handleNetworkError(e, events, attempt)
        }
    }

    /**
     * Handle HTTP response based on status code.
     */
    private fun handleResponse(response: HttpResponse, events: List<Event>, attempt: Int): Boolean {
        return when {
            response.status in 200..299 -> {
                persistBuffer()
                config.hooks.onSendSuccess?.invoke(
                    SendSuccessInfo(batchSize = events.size, status = response.status)
                )
                loggerAdapter.info(
                    "Batch sent successfully",
                    mapOf("status" to response.status, "data" to response.data as? String?),
                )
                true
            }

            response.status in 400..499 -> {
                loggerAdapter.warn(
                    "4xx client error, dropping events",
                    mapOf("status" to response.status, "eventsCount" to events.size)
                )
                persistBuffer()
                config.hooks.onDrop?.invoke(
                    DropInfo(eventCount = events.size, reason = DropReason.CLIENT_ERROR)
                )
                true
            }

            response.status >= 500 -> handleServerError(response.status, events, attempt)

            else -> {
                loggerAdapter.warn(
                    "Unexpected status code, dropping events",
                    mapOf(
                        "status" to response.status,
                        "eventsCount" to events.size,
                        "data" to response.data
                    )
                )
                persistBuffer()
                true
            }
        }
    }

    /**
     * Handle 5xx server errors with retry logic.
     */
    private fun handleServerError(status: Int, events: List<Event>, attempt: Int): Boolean {
        if (attempt < config.maxRetries - 1) {
            loggerAdapter.warn(
                "5xx server error, retrying",
                mapOf(
                    "status" to status,
                    "attempt" to attempt + 1,
                    "maxRetries" to config.maxRetries
                )
            )

            val backoffDelay = calculateBackoffDelay(attempt)
            config.hooks.onRetry?.invoke(RetryInfo(attempt = attempt + 1, delay = backoffDelay))

            if (!sleepBeforeRetry(backoffDelay)) return false

            return sendWithRetry(events, attempt + 1)
        }

        loggerAdapter.error(
            "5xx server error, max retries reached",
            mapOf(
                "status" to status,
                "maxRetries" to config.maxRetries,
                "eventsCount" to events.size
            )
        )

        config.hooks.onSendFailure?.invoke(
            SendFailureInfo(batchSize = events.size, error = "5xx: $status", attempt = attempt + 1)
        )

        return false
    }

    /**
     * Handle network errors with retry logic.
     */
    private fun handleNetworkError(error: Exception, events: List<Event>, attempt: Int): Boolean {
        loggerAdapter.error("Network error occurred", mapOf("error" to error.message))

        if (attempt < config.maxRetries - 1) {
            loggerAdapter.warn(
                "Network error, retrying",
                mapOf(
                    "attempt" to attempt + 1,
                    "maxRetries" to config.maxRetries,
                    "error" to error.message
                )
            )

            val backoffDelay = calculateBackoffDelay(attempt)
            config.hooks.onRetry?.invoke(RetryInfo(attempt = attempt + 1, delay = backoffDelay))

            if (!sleepBeforeRetry(backoffDelay)) return false

            return sendWithRetry(events, attempt + 1)
        }

        val message = error.message ?: error::class.java.simpleName
        loggerAdapter.error(
            "Network error, max retries reached",
            mapOf(
                "maxRetries" to config.maxRetries,
                "eventsCount" to events.size,
                "error" to message
            )
        )

        config.hooks.onSendFailure?.invoke(
            SendFailureInfo(batchSize = events.size, error = message, attempt = attempt + 1)
        )

        return false
    }

    /**
     * Re-queue a failed batch before the current buffer and persist it.
     */
    private fun requeueBatch(batch: List<Event>) {
        storageLock.withLock {
            prependEvents(batch)
            persistEventsLocked(queueSnapshot())
        }
    }

    /**
     * Schedule an automatic one-shot flush after the configured interval.
     */
    private fun scheduleFlush() {
        if (isDisposed.get()) return

        synchronized(scheduleLock) {
            if (scheduledFlushTask != null) return

            scheduledFlushTask = executor.schedule(
                {
                    synchronized(scheduleLock) {
                        scheduledFlushTask = null
                    }
                    flush(true)
                },
                config.flushInterval,
                TimeUnit.MILLISECONDS
            )
        }
    }

    private fun cancelScheduledFlush() {
        synchronized(scheduleLock) {
            scheduledFlushTask?.cancel(false)
            scheduledFlushTask = null
        }
    }

    /**
     * Restore persisted events from storage.
     */
    fun restore() {
        reset()

        try {
            storageLock.withLock {
                val stored = storageAdapter.load()
                replaceQueue(stored)
            }
        } catch (e: Exception) {
            loggerAdapter.error(
                "Failed to restore events from storage",
                mapOf("error" to (e.message ?: e::class.java.simpleName))
            )
        }

        if (getQueueSize() > 0) {
            scheduleFlush()
            loggerAdapter.info("Restored ${getQueueSize()} persisted events")
        }
    }

    fun getQueueSize(): Int = queueLock.withLock { eventQueue.size }

    fun dispose() {
        if (!isDisposed.compareAndSet(false, true)) return

        loggerAdapter.debug("Disposing dispatcher")
        cancelScheduledFlush()

        try {
            val events = drainQueue()
            if (events.isNotEmpty()) {
                storageLock.withLock {
                    storageAdapter.save(events)
                }
                loggerAdapter.info("Persisted ${events.size} events on dispose")
            }
        } catch (e: Exception) {
            loggerAdapter.error("Failed to persist events on dispose: ${e.message}")
        }

        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    private fun reset() {
        isDisposed.set(false)
        isFlushInProgress.set(false)
    }

    private fun persistBufferAsync() {
        try {
            executor.execute {
                if (!isDisposed.get()) {
                    persistBuffer()
                }
            }
        } catch (e: RuntimeException) {
            loggerAdapter.error(
                "Failed to schedule event persistence",
                mapOf("error" to (e.message ?: e::class.java.simpleName))
            )
        }
    }

    private fun persistBuffer() {
        storageLock.withLock {
            persistEventsLocked(queueSnapshot())
        }
    }

    private fun persistEventsLocked(
        events: List<Event>,
        errorMessage: String = "Failed to persist events to storage"
    ) {
        try {
            storageAdapter.save(events)
        } catch (e: Exception) {
            loggerAdapter.error(
                errorMessage,
                mapOf(
                    "error" to (e.message ?: e::class.java.simpleName),
                    "eventsCount" to events.size
                )
            )
        }
    }

    private fun sleepBeforeRetry(delay: Long): Boolean {
        return try {
            Thread.sleep(delay)
            true
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    private fun calculateBackoffDelay(attempt: Int): Long {
        val baseDelay = config.retryOptions.minDelay
        val factor = config.retryOptions.backoffFactor
        val exponentialDelay = (baseDelay * factor.pow(attempt)).toLong()
        val jitter = Random.nextLong(0, 1001)
        return minOf(exponentialDelay + jitter, config.retryOptions.maxDelay)
    }

    private fun serializedSize(event: Event): Long {
        return runCatching {
            json.encodeToString(event).toByteArray(Charsets.UTF_8).size.toLong()
        }.getOrElse {
            event.toString().toByteArray(Charsets.UTF_8).size.toLong()
        }
    }

    private fun replaceQueue(events: List<Event>) {
        queueLock.withLock {
            eventQueue.clear()
            events.takeLast(config.maxBufferSize).forEach { eventQueue.addLast(it) }
        }
    }

    private fun offerEvent(event: Event): Int {
        return queueLock.withLock {
            if (eventQueue.size == config.maxBufferSize) {
                eventQueue.removeFirst()
                config.hooks.onDrop?.invoke(DropInfo(eventCount = 1, reason = DropReason.BUFFER_FULL))
            }
            eventQueue.addLast(event)
            eventQueue.size
        }
    }

    private fun pollEvent(): Event? {
        return queueLock.withLock {
            if (eventQueue.isEmpty()) null else eventQueue.removeFirst()
        }
    }

    private fun drainQueue(): List<Event> {
        return queueLock.withLock {
            val events = eventQueue.toList()
            eventQueue.clear()
            events
        }
    }

    private fun prependEvents(events: List<Event>) {
        queueLock.withLock {
            for (event in events.asReversed()) {
                while (eventQueue.size >= config.maxBufferSize) {
                    eventQueue.removeLast()

                    config.hooks.onDrop?.invoke(
                        DropInfo(
                            eventCount = 1,
                            reason = DropReason.BUFFER_FULL
                        )
                    )
                }

                eventQueue.addFirst(event)
            }
        }
    }

    private fun queueSnapshot(): List<Event> = queueLock.withLock { eventQueue.toList() }

    private fun isQueueEmpty(): Boolean = queueLock.withLock { eventQueue.isEmpty() }

    private fun reportDroppedExpired(droppedCount: Int) {
        if (droppedCount <= 0) return

        config.hooks.onDrop?.invoke(
            DropInfo(eventCount = droppedCount, reason = DropReason.EXPIRED)
        )
        loggerAdapter.warn("Dropped $droppedCount expired events")
    }

    private companion object {
        const val DEFAULT_MAX_PAYLOAD_SIZE = 64L * 1024L
        const val DEFAULT_MAX_BUFFER_SIZE = 50
    }
}
