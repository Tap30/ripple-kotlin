package com.tapsioss.ripple.spring.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.adapters.StorageAdapter
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * File-based storage adapter for Spring
 */
class FileStorageAdapter(
    private val filePath: String = "./ripple_events.json",
    private val objectMapper: ObjectMapper = ObjectMapper()
) : StorageAdapter {
    
    override suspend fun save(events: List<Event>) {
        try {
            val json = objectMapper.writeValueAsString(events)
            Files.write(Paths.get(filePath), json.toByteArray())
        } catch (e: Exception) {
            // Log error but don't throw to prevent event loss
        }
    }

    override suspend fun load(): List<Event> {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                val json = Files.readString(Paths.get(filePath))
                objectMapper.readValue(json, Array<Event>::class.java).toList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun clear() {
        try {
            Files.deleteIfExists(Paths.get(filePath))
        } catch (e: Exception) {
            // Ignore deletion errors
        }
    }
}
