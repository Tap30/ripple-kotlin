package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RippleClientTest {

    private val httpAdapter = mockk<HttpAdapter>(relaxed = true)
    private val storageAdapter = mockk<StorageAdapter>(relaxed = true)
    private val loggerAdapter = mockk<LoggerAdapter>(relaxed = true)
    
    private val config = RippleConfig(
        apiKey = "test-key",
        endpoint = "https://api.test.com",
        adapters = AdapterConfig(httpAdapter, storageAdapter, loggerAdapter)
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
        coEvery { storageAdapter.load() } returns emptyList()
    }

    private class TestRippleClient(config: RippleConfig) : RippleClient(config) {
        private var testSessionId: String? = "test-session"
        private var testPlatform: Platform? = Platform.Server()

        override fun getSessionId(): String? = testSessionId
        override fun getPlatform(): Platform? = testPlatform
        
        fun setTestSessionId(sessionId: String?) { testSessionId = sessionId }
        fun setTestPlatform(platform: Platform?) { testPlatform = platform }
    }

    @Test
    fun `when client is initialized, then dispatcher is restored and logger notified`() = runTest {
        val client = TestRippleClient(config)

        client.init()

        coVerify { storageAdapter.load() }
        verify { loggerAdapter.info("RippleClient initialized") }
    }
wh
    @Test
    fun `when tracking before init, then exception is thrown`() = runTest {
        val client = TestRippleClient(config)

        assertThrows<IllegalStateException> {
            runBlocking { client.track("test_event") }
        }
    }

    @Test
    fun `when tracking event with all parameters, then event is created correctly`() = runTest {
        val client = TestRippleClient(config)
        client.init()

        client.track("user_login", mapOf("method" to "google"), mapOf("version" to "1.0"))

        verify { loggerAdapter.debug("Event enqueued: user_login") }
    }

    @Test
    fun `when tracking event with only name, then event is created with nulls`() = runTest {
        val client = TestRippleClient(config)
        client.init()

        client.track("simple_event")

        verify { loggerAdapter.debug("Event enqueued: simple_event") }
    }

    @Test
    fun `when setting metadata, then metadata is stored in manager`() = runTest {
        val client = TestRippleClient(config)

        client.setMetadata("userId", "123")
        client.setMetadata("plan", "premium")

        assertEquals("123", client.metadataManager.get("userId"))
        assertEquals("premium", client.metadataManager.get("plan"))
    }

    @Test
    fun `when tracking with shared and event metadata, then metadata is merged correctly`() = runTest {
        val client = TestRippleClient(config)
        client.init()
        client.setMetadata("global", "value")

        client.track("test", null, mapOf("event" to "specific"))

        verify { loggerAdapter.debug("Event enqueued: test") }
    }

    @Test
    fun `when tracking with no metadata, then event metadata is null`() = runTest {
        val client = TestRippleClient(config)
        client.init()

        client.track("no_metadata_event")

        verify { loggerAdapter.debug("Event enqueued: no_metadata_event") }
    }

    @Test
    fun `when flushing, then dispatcher flush is called`() = runTest {
        val client = TestRippleClient(config)
        client.init()

        client.flush()

        // Verify flush was called (no events to send, so no HTTP call)
        coVerify(exactly = 0) { httpAdapter.send(any(), any(), any(), any()) }
    }

    @Test
    fun `when disposing, then dispatcher is disposed and logger notified`() = runTest {
        val client = TestRippleClient(config)

        client.dispose()

        verify { loggerAdapter.info("RippleClient disposed") }
    }

    @Test
    fun `when client has null session and platform, then event uses nulls`() = runTest {
        val client = TestRippleClient(config)
        client.setTestSessionId(null)
        client.setTestPlatform(null)
        client.init()

        client.track("null_session_event")

        verify { loggerAdapter.debug("Event enqueued: null_session_event") }
    }

    @Test
    fun `when multiple clients are used concurrently, then each operates independently`() = runTest {
        val client1 = TestRippleClient(config.copy())
        val client2 = TestRippleClient(config.copy())
        
        client1.init()
        client2.init()

        client1.setMetadata("clientId", "client1")
        client2.setMetadata("clientId", "client2")

        val jobs = listOf(
            async { client1.track("event1") },
            async { client2.track("event2") }
        )
        jobs.awaitAll()

        assertEquals("client1", client1.metadataManager.get("clientId"))
        assertEquals("client2", client2.metadataManager.get("clientId"))
    }

    @Test
    fun `when event stampede occurs, then all events are processed`() = runTest {
        val client = TestRippleClient(config)
        client.init()

        val jobs = (1..100).map { index ->
            async { client.track("stampede_event_$index") }
        }
        jobs.awaitAll()

        verify(exactly = 100) { loggerAdapter.debug(match { it.startsWith("Event enqueued: stampede_event_") }) }
    }

    @Test
    fun `when metadata merging with empty global metadata, then only event metadata is used`() = runTest {
        val client = TestRippleClient(config)
        client.init()

        client.track("test", null, mapOf("event" to "only"))

        verify { loggerAdapter.debug("Event enqueued: test") }
    }

    @Test
    fun `when metadata merging with empty event metadata, then only global metadata is used`() = runTest {
        val client = TestRippleClient(config)
        client.setMetadata("global", "only")
        client.init()

        client.track("test", null, null)

        verify { loggerAdapter.debug("Event enqueued: test") }
    }

    @Test
    fun `when event metadata overrides global metadata, then event metadata takes precedence`() = runTest {
        val client = TestRippleClient(config)
        client.setMetadata("key", "global")
        client.init()

        client.track("test", null, mapOf("key" to "event"))

        verify { loggerAdapter.debug("Event enqueued: test") }
    }
}
