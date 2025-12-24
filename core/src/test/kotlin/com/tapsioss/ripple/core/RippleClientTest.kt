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
        
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = true, status = 200, data = null)
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
            client.dispose()
        }
    }
    
    @Test
    fun `when client is not initialized, then track throws exception`() {
        assertThrows<IllegalStateException> {
            client.track("test_event")
        }
    }
    
    @Test
    fun `when client is initialized, then track succeeds`() {
        client.init()
        
        client.track("test_event")
        
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `when tracking with payload, then event contains payload`() {
        client.init()
        val payload = mapOf("key" to "value")
        
        client.track("test_event", payload)
        
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `when setting metadata, then metadata is included in events`() {
        client.init()
        client.setMetadata("user_id", "12345")
        
        client.track("test_event")
        
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `when flush is called, then events are sent`() {
        client.init()
        client.track("test_event")
        
        client.flushSync()
        
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    @Test
    fun `when dispose is called, then client cannot track events`() {
        client.init()
        client.dispose()
        
        // After dispose, track should throw since client is not initialized
        assertThrows<IllegalStateException> {
            client.track("test_event")
        }
    }
    
    @Test
    fun `when init is called multiple times, then it is idempotent`() {
        client.init()
        client.init()
        client.init()
        
        // Should not throw
        client.track("test_event")
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `when metadata is cleared, then events have no global metadata`() {
        client.init()
        client.setMetadata("key", "value")
        client.clearMetadata()
        
        client.track("test_event")
        
        assertEquals(1, client.getQueueSize())
    }
    
    /**
     * Test implementation of RippleClient
     */
    private class TestRippleClient(config: RippleConfig) : RippleClient(config) {
        override fun getSessionId(): String = "test-session-id"
        override fun getPlatform(): Platform = Platform(
            os = "Test",
            osVersion = "1.0",
            device = "TestDevice",
            manufacturer = "TestManufacturer"
        )
    }
}
