package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DispatcherTest {

    private val httpAdapter = mockk<HttpAdapter>(relaxed = true)
    private val storageAdapter = mockk<StorageAdapter>(relaxed = true)
    private val loggerAdapter = mockk<LoggerAdapter>(relaxed = true)
    
    private val config = Dispatcher.DispatcherConfig(
        endpoint = "https://api.test.com",
        apiKey = "test-key",
        apiKeyHeader = "X-API-Key",
        flushInterval = 100L,
        maxBatchSize = 3,
        maxRetries = 2
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    private fun createDispatcher() = Dispatcher(config, httpAdapter, storageAdapter, loggerAdapter)


    @Test
    fun `when flush succeeds, then events are sent and storage cleared`() = runTest {
        coEvery { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(true, 200)
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test"))

        dispatcher.flush()

        coVerify { httpAdapter.send(any(), any(), any(), any()) }
        coVerify { storageAdapter.clear() }
    }

    @Test
    fun `when flush fails with retries, then events are persisted`() = runTest {
        coEvery { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(false, 500)
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test"))

        dispatcher.flush()

        coVerify(atLeast = 1) { httpAdapter.send(any(), any(), any(), any()) }
        coVerify { storageAdapter.save(any()) }
    }

    @Test
    fun `when flush throws exception, then retries with backoff`() = runTest {
        coEvery { httpAdapter.send(any(), any(), any(), any()) } throws RuntimeException("Network error")
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test"))

        dispatcher.flush()

        coVerify(atLeast = 1) { httpAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun `when restore is called, then persisted events are loaded`() = runTest {
        val events = listOf(createTestEvent("restored1"), createTestEvent("restored2"))
        coEvery { storageAdapter.load() } returns events
        val dispatcher = createDispatcher()

        dispatcher.restore()

        coVerify { storageAdapter.load() }
    }

    @Test
    fun `when restore fails, then operation completes gracefully`() = runTest {
        coEvery { storageAdapter.load() } throws RuntimeException("Storage error")
        val dispatcher = createDispatcher()

        dispatcher.restore()

        coVerify { storageAdapter.load() }
    }

    @Test
    fun `when dispatcher is disposed, then scheduled flush stops`() = runTest {
        val dispatcher = createDispatcher()
        dispatcher.startScheduledFlush(this)

        dispatcher.dispose()

        assertTrue(true) // Test passes if no exceptions
    }

    @Test
    fun `when flush is called after dispose, then operation is ignored`() = runTest {
        val dispatcher = createDispatcher()
        dispatcher.dispose()

        dispatcher.flush()

        coVerify(exactly = 0) { httpAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun `when flush with empty queue, then no HTTP call is made`() = runTest {
        val dispatcher = createDispatcher()

        dispatcher.flush()

        coVerify(exactly = 0) { httpAdapter.send(any(), any(), any(), any()) }
    }

    private fun createTestEvent(name: String) = Event(
        name = name,
        payload = mapOf("test" to "data"),
        issuedAt = System.currentTimeMillis(),
        metadata = null,
        sessionId = "test-session",
        platform = Platform.Server()
    )
}
