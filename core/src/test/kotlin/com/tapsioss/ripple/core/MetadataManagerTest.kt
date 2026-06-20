package com.tapsioss.ripple.core

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetadataManagerTest {
    @Test
    fun `metadata starts empty and returns cached immutable snapshots`() {
        val manager = MetadataManager()

        assertTrue(manager.isEmpty())

        manager.set("one", 1)
        val first = manager.getAll()
        val second = manager.getAll()

        assertEquals(mapOf("one" to 1), first)
        assertTrue(first === second)
        assertFalse(manager.isEmpty())
    }

    @Test
    fun `metadata snapshot changes after mutation and clear`() {
        val manager = MetadataManager()

        manager.set("one", 1)
        val before = manager.getAll()
        manager.set("two", 2)
        val after = manager.getAll()

        assertEquals(mapOf("one" to 1), before)
        assertEquals(mapOf("one" to 1, "two" to 2), after)

        manager.clear()
        assertEquals(emptyMap(), manager.getAll())
        assertTrue(manager.isEmpty())
    }

    @Test
    fun `metadata supports concurrent writes`() {
        val manager = MetadataManager()
        val executor = Executors.newFixedThreadPool(4)
        val done = CountDownLatch(100)

        repeat(100) { index ->
            executor.execute {
                manager.set("key-$index", index)
                done.countDown()
            }
        }

        assertTrue(done.await(2, java.util.concurrent.TimeUnit.SECONDS))
        executor.shutdownNow()
        assertEquals(100, manager.getAll().size)
    }
}
