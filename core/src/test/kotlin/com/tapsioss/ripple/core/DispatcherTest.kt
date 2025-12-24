package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
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
    private lateinit var loggerAdapter: LoggerAdapter
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
        loggerAdapter = mockk(relaxed = true)
        
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = true, status = 200, data = null)
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
    fun `when event is enqueued, then queue size increases`() {
        val dispatcher = createDispatcher()
        val event = createTestEvent("test_event")
        
        dispatcher.enqueue(event)
        
        assertEquals(1, dispatcher.getQueueSize())
    }
    
    @Test
    fun `when flush is called, then events are sent via http adapter`() {
        val dispatcher = createDispatcher()
        val event = createTestEvent("test_event")
        
        dispatcher.enqueue(event)
        dispatcher.flushSync()
        
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    @Test
    fun `when flush succeeds, then queue is cleared`() {
        val dispatcher = createDispatcher()
        val event = createTestEvent("test_event")
        
        dispatcher.enqueue(event)
        dispatcher.flushSync()
        
        assertEquals(0, dispatcher.getQueueSize())
    }
    
    @Test
    fun `when flush fails, then events are re-queued`() {
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = false, status = 500, data = null)
        
        val dispatcher = createDispatcher()
        val event = createTestEvent("test_event")
        
        dispatcher.enqueue(event)
        dispatcher.flushSync()
        
        assertTrue(dispatcher.getQueueSize() > 0)
    }
    
    @Test
    fun `when restore is called, then events are loaded from storage`() {
        val storedEvents = listOf(createTestEvent("stored_event"))
        every { storageAdapter.load() } returns storedEvents
        
        val dispatcher = createDispatcher()
        dispatcher.restore()
        
        assertEquals(1, dispatcher.getQueueSize())
    }
    
    @Test
    fun `when dispatcher is disposed, then no more events can be enqueued`() {
        val dispatcher = createDispatcher()
        dispatcher.dispose()
        
        dispatcher.enqueue(createTestEvent("test_event"))
        
        assertEquals(0, dispatcher.getQueueSize())
    }
    
    @Test
    fun `when batch size is reached, then flush is triggered`() {
        val smallBatchConfig = config.copy(maxBatchSize = 2)
        dispatcher = Dispatcher(smallBatchConfig, httpAdapter, storageAdapter, loggerAdapter)
        
        dispatcher.enqueue(createTestEvent("event1"))
        dispatcher.enqueue(createTestEvent("event2"))
        
        // Give async flush time to complete
        Thread.sleep(100)
        
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    private fun createTestEvent(name: String) = Event(
        name = name,
        payload = mapOf("key" to "value"),
        issuedAt = System.currentTimeMillis(),
        metadata = null,
        sessionId = "test-session",
        platform = null
    )
}
