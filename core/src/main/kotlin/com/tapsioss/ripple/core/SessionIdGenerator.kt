package com.tapsioss.ripple.core

/**
 * Session ID generator per API contract format: {timestamp}-{random}
 */
object SessionIdGenerator {
    fun generate(): String {
        val timestamp = System.currentTimeMillis()
        val random = (100000..999999).random()
        return "$timestamp-$random"
    }
}