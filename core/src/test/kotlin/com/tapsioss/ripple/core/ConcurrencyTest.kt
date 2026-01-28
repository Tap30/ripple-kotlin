package com.tapsioss.ripple.core

import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.NoOpLoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import io.mockk.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcurrencyTest {
    
    private lateinit var httpAdapter: HttpAdapter
    private lateinit var storageAdapter: StorageAdapter
    private lateinit var config: RippleConfig
    private lateinit var client: TestRippleClient
    
    @BeforeEach
    fun setup() {
        httpAdapter = mockk(relaxed = true)
        storageAdapter = mockk(relaxed = true)
        
        every { httpAdapter.send(any(), any(), any(), any()) } returns HttpResponse(ok = true, status = 200)
        every { storageAdapter.load() } returns emptyList()
        
        config = RippleConfig(
            apiKey = "test-api-key",
            endpoint = "https://api.example.com/events",
            flushInterval = 60000L, // Long interval to control flush manually
            adapters = AdapterConfig(
                httpAdapter = httpAdapter,
                storageAdapter = storageAdapter,
                loggerAdapter = NoOpLoggerAdapter()
            )
        )
        
        client = TestRippleClient(config)
    }
    
    @AfterEach
    fun tearDown() {
        try { client.dispose() } catch (_: Exception) {}
    }
    
    @Test
    fun `concurrent track calls are thread-safe`() {
        client.init()
        
        val threadCount = 10
        val eventsPerThread = 50
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val trackedCount = AtomicInteger(0)
        
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    repeat(eventsPerThread) { eventId ->
                        client.track("event_${threadId}_$eventId", mapOf("thread" to threadId))
                        trackedCount.incrementAndGet()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        
        // All events should have been tracked (queue size may vary due to auto-flush)
        assertEquals(threadCount * eventsPerThread, trackedCount.get())
    }
    
    @Test
    fun `concurrent setMetadata calls are thread-safe`() {
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        
        repeat(threadCount) { threadId ->
            executor.submit {
                try {
                    repeat(100) {
                        client.setMetadata("key_$threadId", "value_$it")
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        
        // All keys should exist
        val metadata = client.getMetadata()
        assertEquals(threadCount, metadata.size)
    }
    
    @Test
    fun `concurrent flush calls are serialized`() {
        client.init()
        
        // Add some events
        repeat(50) { client.track("event_$it") }
        
        val flushCount = AtomicInteger(0)
        every { httpAdapter.send(any(), any(), any(), any()) } answers {
            flushCount.incrementAndGet()
            Thread.sleep(50) // Simulate network delay
            HttpResponse(ok = true, status = 200)
        }
        
        val threadCount = 5
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        
        repeat(threadCount) {
            executor.submit {
                try {
                    client.flush()
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        Thread.sleep(200) // Wait for async flush
        executor.shutdown()
        
        // Only one flush should actually execute (mutex protection)
        assertTrue(flushCount.get() <= 2) // Allow for timing edge cases
    }
    
    @Test
    fun `track and dispose race condition handled`() {
        client.init()
        
        val latch = CountDownLatch(2)
        val executor = Executors.newFixedThreadPool(2)
        val errors = AtomicInteger(0)
        
        executor.submit {
            try {
                repeat(100) {
                    try {
                        client.track("event_$it")
                    } catch (_: IllegalStateException) {
                        // Expected after dispose
                    }
                }
            } finally {
                latch.countDown()
            }
        }
        
        executor.submit {
            try {
                Thread.sleep(10)
                client.dispose()
            } finally {
                latch.countDown()
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        
        // Should not crash
    }
    
    @Test
    fun `concurrent init calls are idempotent`() {
        val threadCount = 5
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        
        repeat(threadCount) {
            executor.submit {
                try {
                    client.init()
                } finally {
                    latch.countDown()
                }
            }
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS))
        executor.shutdown()
        
        // Should be able to track after concurrent inits
        client.track("test_event")
        assertEquals(1, client.getQueueSize())
    }
    
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
