package com.tapsioss.ripple.sample.android

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RippleE2ETest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val serverClient = TestServerClient()

    @Before
    fun setup() {
        serverClient.clearEvents()
        serverClient.setServerError(false)
    }

    @After
    fun teardown() {
        serverClient.clearEvents()
    }

    @Test
    fun trackEvent_sendsToServer() {
        composeRule.onNodeWithText("Track Event").performClick()
        composeRule.onNodeWithText("Flush").performClick()

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue("Expected at least 1 event", events.isNotEmpty())
        assertEquals("test_event", events.last()["name"])
    }

    @Test
    fun setMetadata_attachedToEvents() {
        composeRule.onNodeWithText("Set Metadata").performClick()
        composeRule.onNodeWithText("Track Event").performClick()
        composeRule.onNodeWithText("Flush").performClick()

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue(events.isNotEmpty())
        val metadata = events.last()["metadata"] as? Map<*, *>
        assertEquals("test-user-123", metadata?.get("user_id"))
    }

    @Test
    fun batchEvents_sentTogether() {
        repeat(3) {
            composeRule.onNodeWithText("Track Event").performClick()
        }
        composeRule.onNodeWithText("Flush").performClick()

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue("Expected 3 events", events.size >= 3)
    }

    @Test
    fun clearMetadata_removesFromEvents() {
        composeRule.onNodeWithText("Set Metadata").performClick()
        composeRule.onNodeWithText("Clear Metadata").performClick()
        composeRule.onNodeWithText("Track Event").performClick()
        composeRule.onNodeWithText("Flush").performClick()

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue(events.isNotEmpty())
        val metadata = events.last()["metadata"] as? Map<*, *>
        assertTrue(metadata?.get("user_id") == null)
    }
}
