package com.tapsioss.ripple.android

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.tapsioss.ripple.core.AdapterConfig
import com.tapsioss.ripple.core.AppState
import com.tapsioss.ripple.core.DefaultRippleEvent
import com.tapsioss.ripple.core.DefaultRippleMetadata
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.RippleConfig
import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonPrimitive

class AndroidRippleClientTest {
    @Test
    fun `context constructor persists anonymous and user identity`() {
        val prefs = sharedPreferences()
        val context = contextWithPreferences(prefs)
        val firstHttp = RecordingHttpAdapter()
        val first = AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata>(
            context,
            config(firstHttp)
        )

        first.init()
        val anonymousId = first.getAnonymousId()
        first.identify("user-1")
        eventually { firstHttp.requests.isNotEmpty() }
        first.dispose()

        val second = AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata>(
            context,
            config(RecordingHttpAdapter())
        )

        second.init()

        assertEquals(anonymousId, second.getAnonymousId())
        assertEquals("user-1", second.getUserId())
    }

    @Test
    fun `android screen helper fills payload from activity`() {
        val http = RecordingHttpAdapter()
        val client = AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata>(config(http))
        val activity = mockk<Activity>()
        every { activity.title } returns "Checkout"
        every { activity.packageName } returns "com.example"
        every { activity.localClassName } returns "CheckoutActivity"

        client.screen(activity)

        eventually { http.requests.isNotEmpty() }
        val event = http.requests.single().events.single()

        assertEquals("screened", event.name)
        assertEquals("Checkout", event.payload?.get("title")?.jsonPrimitive?.content)
        assertEquals("android://com.example/CheckoutActivity", event.payload?.get("url")?.jsonPrimitive?.content)
        assertEquals("CheckoutActivity", event.payload?.get("pathname")?.jsonPrimitive?.content)
    }

    @Test
    fun `android lifecycle callbacks track foreground and background app states`() {
        val prefs = sharedPreferences()
        val application = mockk<Application>()
        every { application.applicationContext } returns application
        every { application.getSharedPreferences(any(), any()) } returns prefs
        val callbackSlot: CapturingSlot<Application.ActivityLifecycleCallbacks> = slot()
        every { application.registerActivityLifecycleCallbacks(capture(callbackSlot)) } just runs
        every { application.unregisterActivityLifecycleCallbacks(any()) } just runs
        val http = RecordingHttpAdapter()
        val client = AndroidRippleClient<DefaultRippleEvent, DefaultRippleMetadata>(
            application,
            config(http)
        )
        val activity = mockk<Activity>(relaxed = true)

        client.init()
        callbackSlot.captured.onActivityStarted(activity)
        callbackSlot.captured.onActivityStopped(activity)

        eventually { http.requests.flatMap { it.events }.size >= 2 }
        val events = http.requests.flatMap { it.events }

        assertEquals(listOf("app_state_changed", "app_state_changed"), events.map { it.name })
        assertEquals("foreground", events[0].payload?.get("newState")?.jsonPrimitive?.content)
        assertEquals("background", events[1].payload?.get("newState")?.jsonPrimitive?.content)

        client.dispose()
    }

    private fun contextWithPreferences(prefs: SharedPreferences): Context {
        val context = mockk<Context>()
        every { context.applicationContext } returns context
        every { context.getSharedPreferences(any(), any()) } returns prefs
        return context
    }

    private fun sharedPreferences(): SharedPreferences {
        val values = mutableMapOf<String, String?>()
        val prefs = mockk<SharedPreferences>()
        val editor = mockk<SharedPreferences.Editor>()

        every { prefs.getString(any(), any()) } answers {
            values[firstArg()] ?: secondArg()
        }
        every { editor.putString(any(), any()) } answers {
            values[firstArg()] = secondArg()
            editor
        }
        every { editor.remove(any()) } answers {
            values.remove(firstArg<String>())
            editor
        }
        every { editor.apply() } just runs
        every { prefs.edit() } returns editor

        return prefs
    }

    private fun config(http: RecordingHttpAdapter): RippleConfig {
        return RippleConfig(
            apiKey = "api-key",
            endpoint = "https://example.test/events",
            adapters = AdapterConfig(
                httpAdapter = http,
                storageAdapter = NoOpStorageAdapter(),
                loggerAdapter = NoOpLoggerAdapter()
            ),
            maxBatchSize = 1
        )
    }

    private fun eventually(timeoutMillis: Long = 1_000, assertion: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (assertion()) return
            Thread.sleep(10)
        }
        check(assertion()) { "Condition was not met within ${timeoutMillis}ms" }
    }
}

private class RecordingHttpAdapter : HttpAdapter {
    data class Request(
        val endpoint: String,
        val events: List<Event>
    )

    val requests = CopyOnWriteArrayList<Request>()

    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        requests += Request(endpoint, events.toList())
        return HttpResponse(ok = true, status = 200)
    }
}

private class NoOpStorageAdapter : StorageAdapter {
    override fun save(events: List<Event>) = Unit
    override fun load(): List<Event> = emptyList()
    override fun clear() = Unit
}

private class NoOpLoggerAdapter : LoggerAdapter {
    override fun debug(message: String, vararg args: Any?) = Unit
    override fun info(message: String, vararg args: Any?) = Unit
    override fun warn(message: String, vararg args: Any?) = Unit
    override fun error(message: String, vararg args: Any?) = Unit
}
