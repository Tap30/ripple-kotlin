package com.tapsioss.ripple.core

import kotlinx.coroutines.sync.Mutex as CoroutineMutex
import kotlinx.coroutines.sync.withLock

/**
 * Mutual exclusion lock for preventing race conditions
 */
class Mutex {
    private val mutex = CoroutineMutex()

    /**
     * Execute task with exclusive lock
     */
    suspend fun <T> runAtomic(task: suspend () -> T): T {
        return mutex.withLock { task() }
    }
}
