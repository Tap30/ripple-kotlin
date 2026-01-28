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
import kotlin.test.assertNull
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
        try { client.dispose() } catch (_: Exception) {}
    }
    
    // ==================== INITIALIZATION ====================
    
    @Test
    fun `track throws when not initialized`() {
        assertThrows<IllegalStateException> { client.track("test_event") }
    }
    
    @Test
    fun `init is idempotent`() {
        client.init()
        val sessionId = client.getSessionId()
        client.init()
        assertEquals(sessionId, client.getSessionId())
    }
    
    @Test
    fun `getSessionId returns null before init`() {
        assertNull(client.getSessionId())
    }
    
    @Test
    fun `getSessionId returns value after init`() {
        client.init()
        assertNotNull(client.getSessionId())
        assertTrue(client.getSessionId()!!.contains("-"))
    }

    // ==================== TYPED TRACK ====================
    
    @Test
    fun `track with typed event`() {
        client.init()
        client.track(TestEvent.Login("test@example.com"))
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `track with typed event and typed metadata`() {
        client.init()
        client.track(TestEvent.Login("test@example.com"), TestMetadata("user-123"))
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `track with typed event and untyped metadata`() {
        client.init()
        client.track(TestEvent.Login("test@example.com"), mapOf("key" to "value"))
        assertEquals(1, client.getQueueSize())
    }
    
    // ==================== UNTYPED TRACK ====================
    
    @Test
    fun `track with name only`() {
        client.init()
        client.track("test_event")
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `track with name and payload`() {
        client.init()
        client.track("test_event", mapOf("key" to "value"))
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `track with name payload and metadata`() {
        client.init()
        client.track("test_event", mapOf("key" to "value"), mapOf("meta" to "data"))
        assertEquals(1, client.getQueueSize())
    }
    
    @Test
    fun `track untyped with typed metadata`() {
        client.init()
        client.track("test_event", mapOf("key" to "value"), TestMetadata("user-123"))
        assertEquals(1, client.getQueueSize())
    }
    
    // ==================== METADATA ====================
    
    @Test
    fun `setMetadata with key value`() {
        client.setMetadata("user_id", "12345")
        assertEquals("12345", client.getMetadata()["user_id"])
    }
    
    @Test
    fun `setMetadata with typed metadata`() {
        client.setMetadata(TestMetadata("user-123"))
        assertEquals("user-123", client.getMetadata()["userId"])
    }
    
    @Test
    fun `clearMetadata removes all`() {
        client.setMetadata("key", "value")
        client.clearMetadata()
        assertTrue(client.getMetadata().isEmpty())
    }
    
    @Test
    fun `removeMetadata removes single key`() {
        client.setMetadata("key1", "value1")
        client.setMetadata("key2", "value2")
        client.removeMetadata("key1")
        assertNull(client.getMetadata()["key1"])
        assertEquals("value2", client.getMetadata()["key2"])
    }
    
    // ==================== FACTORY METHOD ====================
    
    @Test
    fun `create factory method works`() {
        val untypedClient = RippleClient.create(config)
        untypedClient.init()
        untypedClient.track("test_event", mapOf("key" to "value"))
        assertEquals(1, untypedClient.getQueueSize())
    }
    
    // ==================== FLUSH ====================
    
    @Test
    fun `flush sends events`() {
        client.init()
        client.track("test_event")
        client.flushSync()
        verify { httpAdapter.send(any(), any(), any(), any()) }
    }
    
    // ==================== DISPOSE ====================
    
    @Test
    fun `dispose clears state`() {
        client.init()
        client.setMetadata("key", "value")
        client.dispose()
        
        assertTrue(client.getMetadata().isEmpty())
        assertNull(client.getSessionId())
    }
    
    @Test
    fun `dispose prevents tracking`() {
        client.init()
        client.dispose()
        assertThrows<IllegalStateException> { client.track("test_event") }
    }
    
    @Test
    fun `init after dispose works`() {
        client.init()
        client.dispose()
        client.init()
        client.track("test_event")
        assertEquals(1, client.getQueueSize())
    }
    
    // ==================== TEST HELPERS ====================
    
    private class TestRippleClient(config: RippleConfig) : RippleClient<TestEvent, TestMetadata>(config) {
        override fun getPlatform(): Platform = Platform.Server
    }
    
    sealed class TestEvent : RippleEvent {
        data class Login(val email: String) : TestEvent() {
            override val name = "user.login"
            override fun toPayload() = mapOf("email" to email)
        }
    }
    
    data class TestMetadata(val userId: String) : RippleMetadata {
        override fun toMap() = mapOf("userId" to userId)
    }
}
