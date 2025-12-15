package com.tapsioss.ripple.android

import java.util.*

/**
 * Manages session lifecycle for Android
 */
class SessionManager {
    
    private var sessionId: String? = null

    fun initSession() {
        sessionId = generateSessionId()
    }

    fun getSessionId(): String? = sessionId

    private fun generateSessionId(): String = UUID.randomUUID().toString()
}
