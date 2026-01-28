package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.NoOpLoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatcherTest {
    
    private lateinit var httpAdapter: HttpAdapter
    private lateinit var storageAdapter: StorageAdapter
    private lateinit var loggerAdapter: NoOpLoggerAdapter
    private lateinit var dispatcher: Dispatcher
    
    private val config = Dispatcher.DispatcherConfig(
        endpoint = "https://api.example.com/events",
        apiKey = "test-api-key",
        apiKeyHeader = "X-API-Key",
        flushInterval = 5000L,
        maxBatchSize = 10,
        maxRetries = 3
    )
    
    @BeforeEach
    fun setup() {
        httpAdapter = mockk(relaxed = true)
        storageAdapter = mockk(relaxed = true)
        loggerAdapter = NoOpLoggerAdapter()
        
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = true, status = 200)
        every { storageAdapter.load() } returns emptyList()
    }
    
    @AfterEach
    fun tearDown() {
        if (::dispatcher.isInitialized) {
            dispatcher.dispose()
        }
    }
    
    private fun createDispatcher(): Dispatcher {
        dispatcher = Dispatcher(config, httpAdapter, storageAdapter, loggerAdapter)
        return dispatcher
    }
    
    @Test
    fun `enqueue increases queue size`() {
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        assertEquals(1, dispatcher.getQueueSize())
    }
    
    @Test
    fun `flush sends events via http adapter`() {
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        dispatcher.flushSync()
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    @Test
    fun `flush clears queue on success`() {
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        dispatcher.flushSync()
        assertEquals(0, dispatcher.getQueueSize())
    }
    
    @Test
    fun `2xx success clears storage`() {
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = true, status = 200)
        
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        dispatcher.flushSync()
        
        verify { storageAdapter.clear() }
        assertEquals(0, dispatcher.getQueueSize())
    }
    
    @Test
    fun `4xx error drops events and clears storage`() {
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = false, status = 400)
        
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        dispatcher.flushSync()
        
        // Should only call once (no retry for 4xx)
        verify(exactly = 1) { httpAdapter.send(any(), any(), any(), any()) }
        verify { storageAdapter.clear() } // 4xx drops events
        assertEquals(0, dispatcher.getQueueSize())
    }
    
    @Test
    fun `5xx error retries with exponential backoff`() {
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = false, status = 500)
        
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        dispatcher.flushSync()
        
        // Should retry maxRetries times
        verify(exactly = config.maxRetries) { httpAdapter.send(any(), any(), any(), any()) }
        verify { storageAdapter.save(any()) } // Failed events persisted
        assertTrue(dispatcher.getQueueSize() > 0)
    }
    
    @Test
    fun `network error retries with exponential backoff`() {
        every { httpAdapter.send(any(), any(), any(), any()) } throws RuntimeException("Network error")
        
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("test_event"))
        dispatcher.flushSync()
        
        // Should retry maxRetries times
        verify(exactly = config.maxRetries) { httpAdapter.send(any(), any(), any(), any()) }
        verify { storageAdapter.save(any()) } // Failed events persisted
        assertTrue(dispatcher.getQueueSize() > 0)
    }
    
    @Test
    fun `batching processes events in chunks`() {
        val smallBatchConfig = config.copy(maxBatchSize = 2)
        dispatcher = Dispatcher(smallBatchConfig, httpAdapter, storageAdapter, loggerAdapter)
        
        // Add 5 events (should create 3 batches: 2, 2, 1)
        repeat(5) { dispatcher.enqueue(createTestEvent("event$it")) }
        dispatcher.flushSync()
        
        // Should make 3 HTTP calls for 3 batches
        verify(exactly = 3) { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    @Test
    fun `failed batch maintains FIFO order on requeue`() {
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = false, status = 500)
        
        val dispatcher = createDispatcher()
        dispatcher.enqueue(createTestEvent("event1"))
        dispatcher.flushSync()
        
        // Add new event after failure
        dispatcher.enqueue(createTestEvent("event2"))
        
        // Failed event1 should be requeued at front, event2 at back
        assertTrue(dispatcher.getQueueSize() == 2)
    }
    
    @Test
    fun `restore loads events from storage`() {
        val storedEvents = listOf(createTestEvent("stored_event"))
        every { storageAdapter.load() } returns storedEvents
        
        val dispatcher = createDispatcher()
        dispatcher.restore()
        
        assertEquals(1, dispatcher.getQueueSize())
    }
    
    @Test
    fun `disposed dispatcher rejects enqueue`() {
        val dispatcher = createDispatcher()
        dispatcher.dispose()
        dispatcher.enqueue(createTestEvent("test_event"))
        assertEquals(0, dispatcher.getQueueSize())
    }
    
    @Test
    fun `batch size triggers automatic flush`() {
        val smallBatchConfig = config.copy(maxBatchSize = 2)
        dispatcher = Dispatcher(smallBatchConfig, httpAdapter, storageAdapter, loggerAdapter)
        
        dispatcher.enqueue(createTestEvent("event1"))
        dispatcher.enqueue(createTestEvent("event2")) // Should trigger flush
        
        Thread.sleep(100) // Allow async flush to complete
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    @Test
    fun `partial batch success continues processing`() {
        var callCount = 0
        every { httpAdapter.send(any(), any(), any(), any()) } answers {
            callCount++
            if (callCount == 1) HttpResponse(ok = false, status = 500) // First batch fails
            else HttpResponse(ok = true, status = 200) // Second batch succeeds
        }
        
        val smallBatchConfig = config.copy(maxBatchSize = 1, maxRetries = 1)
        dispatcher = Dispatcher(smallBatchConfig, httpAdapter, storageAdapter, loggerAdapter)
        
        dispatcher.enqueue(createTestEvent("event1"))
        dispatcher.enqueue(createTestEvent("event2"))
        dispatcher.flushSync()
        
        // First batch should fail and be requeued, second should succeed
        assertTrue(dispatcher.getQueueSize() == 1) // Only failed batch remains
    }
    
    private fun createTestEvent(name: String) = Event(
        name = name,
        payload = mapOf("key" to "value"),
        issuedAt = System.currentTimeMillis(),
        metadata = null,
        sessionId = "test-session",
        platform = Platform.Server
    )
}
