package cab.tapsi.ripple.spring.adapters.storage

import cab.tapsi.ripple.core.Event
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class FileStorageAdapterTest {
    
    private lateinit var storagePath: Path
    
    @BeforeEach
    fun setup() {
        storagePath = Files.createTempDirectory("ripple-test").resolve("events.json")
    }
    
    @AfterEach
    fun cleanup() {
        storagePath.toFile().delete()
        storagePath.parent.toFile().delete()
    }
    
    @Test
    fun `save and load events without TTL`() {
        val adapter = FileStorageAdapter(storagePath)
        val events = listOf(Event("test", null, System.currentTimeMillis(), null, null))
        
        adapter.save(events)
        val loaded = adapter.load()
        
        assertEquals(1, loaded.size)
        assertEquals("test", loaded[0].name)
    }
    
    @Test
    fun `load returns empty list when no events saved`() {
        val adapter = FileStorageAdapter(storagePath)
        assertTrue(adapter.load().isEmpty())
    }
    
    @Test
    fun `clear removes saved events`() {
        val adapter = FileStorageAdapter(storagePath)
        adapter.save(listOf(Event("test", null, System.currentTimeMillis(), null, null)))
        
        adapter.clear()
        
        assertTrue(adapter.load().isEmpty())
    }
    
    @Test
    fun `load returns events within TTL`() {
        val adapter = FileStorageAdapter(storagePath, ttl = 10_000L)
        val events = listOf(Event("test", null, System.currentTimeMillis(), null, null))
        
        adapter.save(events)
        val loaded = adapter.load()
        
        assertEquals(1, loaded.size)
    }
    
    @Test
    fun `load returns empty and clears when TTL expired`() {
        val adapter = FileStorageAdapter(storagePath, ttl = 1L)
        adapter.save(listOf(Event("test", null, System.currentTimeMillis(), null, null)))
        
        Thread.sleep(10)
        
        assertTrue(adapter.load().isEmpty())
        assertFalse(storagePath.toFile().exists())
    }
    
    @Test
    fun `save empty list does nothing`() {
        val adapter = FileStorageAdapter(storagePath)
        adapter.save(emptyList())
        assertFalse(storagePath.toFile().exists())
    }
}
