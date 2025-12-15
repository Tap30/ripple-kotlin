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
        
        assertEquals("value1", manager.get("key1"))
    }

    @Test
    fun `when getting non-existent key, then returns null`() {
        val manager = MetadataManager()
        
        val result = manager.get("nonexistent")
        
        assertNull(result)
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
        assertNull(manager.get("key1"))
        assertNull(manager.get("key2"))
    }

    @Test
    fun `when overwriting existing key, then new value replaces old`() {
        val manager = MetadataManager()
        manager.set("key", "oldValue")
        
        manager.set("key", "newValue")
        
        assertEquals("newValue", manager.get("key"))
    }
}
