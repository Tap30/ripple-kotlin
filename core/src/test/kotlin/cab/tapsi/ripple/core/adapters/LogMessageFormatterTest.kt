package cab.tapsi.ripple.core.adapters

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LogMessageFormatterTest {
    @Test
    fun `format appends args to message`() {
        val message = LogMessageFormatter.format(
            "Network error occurred",
            mapOf("error" to "timeout")
        )

        assertEquals("Network error occurred {error=timeout}", message)
    }

    @Test
    fun `format leaves message unchanged when args are empty`() {
        assertEquals("Network error occurred", LogMessageFormatter.format("Network error occurred"))
    }
}
