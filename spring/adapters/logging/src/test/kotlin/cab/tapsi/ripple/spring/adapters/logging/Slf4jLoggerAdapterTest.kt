package cab.tapsi.ripple.spring.adapters.logging

import cab.tapsi.ripple.core.adapters.LogLevel
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.slf4j.Logger

class Slf4jLoggerAdapterTest {
    @Test
    fun `error appends args before forwarding to slf4j`() {
        val logger = mockk<Logger>(relaxed = true)
        every { logger.isErrorEnabled } returns true
        val adapter = Slf4jLoggerAdapter(LogLevel.ERROR, logger)

        adapter.error("Network error occurred", mapOf("error" to "timeout"))

        verify { logger.error("Network error occurred {error=timeout}") }
    }
}
