package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RippleClientTest {
    
    private lateinit var httpAdapter: HttpAdapter
    private lateinit var storageAdapter: StorageAdapter
    private lateinit var loggerAdapter: LoggerAdapter
    private lateinit var config: RippleConfig
    private lateinit var client: TestRippleClient
    
    @BeforeEach
    fun setup() {
        httpAdapter = mockk(relaxed = true)
        storageAdapter = mockk(relaxed = true)
        loggerAdapter = mockk(relaxed = true)
        
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = true, status = 200)
        every { storageAdapter.load() } returns emptyList()
        
        config = RippleConfig(
            apiKey = "test-api-key",
            endpoint = "https://api.example.com/events",
            adapters = AdapterConfig(
                httpAdapter = httpAdapter,
                storageAdapter = storageAdapter,
                loggerAdapter = loggerAdapter
            )
        )
        
        client = TestRippleClient(config)
    }
    
    @AfterEach
    fun tearDown() {
        if (::client.isInitialized) {
            try { client.dispose() } catch (_: Exception) {}
        }
    }
    
    @Test
    fun `track throws when not initialized`() {
        assertThrows<IllegalStateException> { client.track("test_event") }
    }
    
    @Test
    fun `track succeeds when initialized`() {
        client.init()
        client.track("test_event")
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `track with payload works`() {
        client.init()
        client.track("test_event", mapOf("key" to "value"))
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `setMetadata and getMetadata work`() {
        client.setMetadata("user_id", "12345")
        val metadata = client.getMetadata()
        assertEquals("12345", metadata["user_id"])
    }
    
    @Test
    fun `flush sends events`() {
        client.init()
        client.track("test_event")
        client.flushSync()
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    @Test
    fun `dispose prevents tracking`() {
        client.init()
        client.dispose()
        assertThrows<IllegalStateException> { client.track("test_event") }
    }
    
    @Test
    fun `init is idempotent`() {
        client.init()
        client.init()
        client.init()
        client.track("test_event")
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `clearMetadata removes all metadata`() {
        client.setMetadata("key", "value")
        client.clearMetadata()
        assertTrue(client.getMetadata().isEmpty())
    }
    
    @Test
    fun `getSessionId returns session ID`() {
        assertNotNull(client.getSessionId())
        assertTrue(client.getSessionId()!!.contains("-"))
    }
    
    @Test
    fun `init after dispose works`() {
        client.init()
        client.track("event1")
        client.dispose()
        
        // Re-initialize
        client.init()
        client.track("event2")
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `multiple init-dispose cycles work`() {
        repeat(3) {
            client.init()
            client.track("event_$it")
            client.flushSync()
            client.dispose()
        }
        // Should not throw
    }
    
    private class TestRippleClient(config: RippleConfig) : RippleClient(config) {
        override fun getSessionId(): String = SessionIdGenerator.generate()
        override fun getPlatform(): Platform = Platform.Server
    }
}
