package com.tapsioss.ripple.sample.android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
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
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

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
        onView(withId(R.id.btnTrackEvent)).perform(click())
        onView(withId(R.id.btnFlush)).perform(click())

        Thread.sleep(2000) // Wait for network

        val events = serverClient.getEvents()
        assertTrue("Expected at least 1 event", events.isNotEmpty())
        assertEquals("test_event", events.last()["name"])
    }

    @Test
    fun setMetadata_attachedToEvents() {
        onView(withId(R.id.btnSetMetadata)).perform(click())
        onView(withId(R.id.btnTrackEvent)).perform(click())
        onView(withId(R.id.btnFlush)).perform(click())

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue(events.isNotEmpty())
        val metadata = events.last()["metadata"] as? Map<*, *>
        assertEquals("test-user-123", metadata?.get("user_id"))
    }

    @Test
    fun batchEvents_sentTogether() {
        repeat(3) {
            onView(withId(R.id.btnTrackEvent)).perform(click())
        }
        onView(withId(R.id.btnFlush)).perform(click())

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue("Expected 3 events", events.size >= 3)
    }

    @Test
    fun clearMetadata_removesFromEvents() {
        onView(withId(R.id.btnSetMetadata)).perform(click())
        onView(withId(R.id.btnClearMetadata)).perform(click())
        onView(withId(R.id.btnTrackEvent)).perform(click())
        onView(withId(R.id.btnFlush)).perform(click())

        Thread.sleep(2000)

        val events = serverClient.getEvents()
        assertTrue(events.isNotEmpty())
        val metadata = events.last()["metadata"] as? Map<*, *>
        assertTrue(metadata?.get("user_id") == null)
    }
}
