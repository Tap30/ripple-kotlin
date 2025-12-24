package com.tapsioss.ripple.spring.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.adapters.StorageAdapter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * File-based storage adapter for Spring applications.
 * 
 * Persists events to a JSON file for offline support and retry.
 * Thread-safe through file locking.
 * 
 * @param storagePath Path to the storage file (default: ripple_events.json in temp directory)
 * @param objectMapper Jackson ObjectMapper for JSON serialization
 */
class FileStorageAdapter(
    private val storagePath: Path = Files.createTempDirectory("ripple").resolve("events.json"),
    private val objectMapper: ObjectMapper = ObjectMapper().registerKotlinModule()
) : StorageAdapter {
    
    private val file: File = storagePath.toFile()
    private val lock = Any()

    override fun save(events: List<Event>) {
        if (events.isEmpty()) return
        
        synchronized(lock) {
            try {
                file.parentFile?.mkdirs()
                objectMapper.writeValue(file, events)
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    override fun load(): List<Event> {
        synchronized(lock) {
            return try {
                if (!file.exists()) return emptyList()
                objectMapper.readValue(file)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    override fun clear() {
        synchronized(lock) {
            try {
                file.delete()
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }
}
