package com.tapsioss.ripple.core

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MetadataManagerTest {

    @Test
    fun `when setting metadata, then value is stored`() {
        val manager = MetadataManager()
        
        manager.set("key1", "value1")
        
        assertEquals("value1", manager.getAll()["key1"])
    }

    @Test
    fun `when getting all metadata, then returns all stored values`() {
        val manager = MetadataManager()
        manager.set("key1", "value1")
        manager.set("key2", 42)
        
        val result = manager.getAll()
        
        assertEquals(mapOf("key1" to "value1", "key2" to 42), result)
    }

    @Test
    fun `when manager is empty, then isEmpty returns true`() {
        val manager = MetadataManager()
        
        assertTrue(manager.isEmpty())
    }

    @Test
    fun `when manager has data, then isEmpty returns false`() {
        val manager = MetadataManager()
        manager.set("key", "value")
        
        assertFalse(manager.isEmpty())
    }

    @Test
    fun `when clearing metadata, then all data is removed`() {
        val manager = MetadataManager()
        manager.set("key1", "value1")
        manager.set("key2", "value2")
        
        manager.clear()
        
        assertTrue(manager.isEmpty())
    }

    @Test
    fun `when overwriting existing key, then new value replaces old`() {
        val manager = MetadataManager()
        manager.set("key", "oldValue")
        
        manager.set("key", "newValue")
        
        assertEquals("newValue", manager.getAll()["key"])
    }
    
    @Test
    fun `merge returns null when both empty`() {
        val manager = MetadataManager()
        
        assertNull(manager.merge(null))
        assertNull(manager.merge(emptyMap()))
    }
    
    @Test
    fun `merge returns event metadata when global empty`() {
        val manager = MetadataManager()
        val eventMeta = mapOf("event" to "value")
        
        assertEquals(eventMeta, manager.merge(eventMeta))
    }
    
    @Test
    fun `merge returns global metadata when event empty`() {
        val manager = MetadataManager()
        manager.set("global", "value")
        
        assertEquals(mapOf("global" to "value"), manager.merge(null))
    }
    
    @Test
    fun `merge combines both with event taking precedence`() {
        val manager = MetadataManager()
        manager.set("shared", "global")
        manager.set("global", "only")
        
        val result = manager.merge(mapOf("shared" to "event", "event" to "only"))
        
        assertEquals("event", result?.get("shared"))
        assertEquals("only", result?.get("global"))
        assertEquals("only", result?.get("event"))
    }
}
