package com.tapsioss.ripple.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RippleConfigTest {
    @Test
    fun `config resolves legacy values when batch and retry options are absent`() {
        val config = testConfig(
            flushInterval = 123L,
            maxBatchSize = 7,
            maxRetries = 5,
            batchOptions = BatchOptions(),
            retryOptions = RetryOptions()
        )

        assertEquals(123L, config.resolvedFlushInterval)
        assertEquals(7, config.resolvedMaxBatchSize)
        assertEquals(65_536L, config.resolvedMaxPayloadSize)
        assertEquals(5, config.resolvedMaxRetries)
    }

    @Test
    fun `config resolves v2 options before legacy values`() {
        val config = testConfig(
            flushInterval = 123L,
            maxBatchSize = 7,
            maxRetries = 5,
            batchOptions = BatchOptions(interval = 456L, size = 3, maxPayloadSize = 99L),
            retryOptions = RetryOptions(maxAttempts = 2)
        )

        assertEquals(456L, config.resolvedFlushInterval)
        assertEquals(3, config.resolvedMaxBatchSize)
        assertEquals(99L, config.resolvedMaxPayloadSize)
        assertEquals(2, config.resolvedMaxRetries)
    }

    @Test
    fun `config rejects invalid required values`() {
        assertFailsWith<IllegalArgumentException> { testConfig().copy(apiKey = "") }
        assertFailsWith<IllegalArgumentException> { testConfig().copy(endpoint = "") }
    }

    @Test
    fun `config rejects invalid batch retry and buffer values`() {
        assertFailsWith<IllegalArgumentException> {
            testConfig(batchOptions = BatchOptions(interval = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(batchOptions = BatchOptions(size = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(batchOptions = BatchOptions(maxPayloadSize = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(retryOptions = RetryOptions(maxAttempts = -1))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(retryOptions = RetryOptions(minDelay = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(retryOptions = RetryOptions(maxDelay = 0))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(retryOptions = RetryOptions(backoffFactor = 0.0))
        }
        assertFailsWith<IllegalArgumentException> {
            testConfig(maxBufferSize = 0)
        }
    }
}
