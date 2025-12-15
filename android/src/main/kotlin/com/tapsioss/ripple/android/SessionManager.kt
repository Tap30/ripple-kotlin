package com.tapsioss.ripple.android

import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import java.util.*

/**
 * Manages session lifecycle for Android
 */
class SessionManager(private val context: Context) : DefaultLifecycleObserver {
    
    private var sessionId: String? = null
    private val prefs = context.getSharedPreferences("ripple_session", Context.MODE_PRIVATE)

    fun initSession() {
        sessionId = generateSessionId()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun getSessionId(): String? = sessionId

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        if (sessionId == null) {
            sessionId = generateSessionId()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // Keep session ID for background events
    }

    fun dispose() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
    }

    private fun generateSessionId(): String = UUID.randomUUID().toString()
}
