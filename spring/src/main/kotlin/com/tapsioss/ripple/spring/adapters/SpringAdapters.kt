package com.tapsioss.ripple.spring.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import com.tapsioss.ripple.core.adapters.LogLevel
import com.tapsioss.ripple.core.adapters.LoggerAdapter
import com.tapsioss.ripple.core.adapters.StorageAdapter
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * WebClient-based HTTP adapter for Spring
 */
class WebClientAdapter(
    private val webClient: WebClient = WebClient.create(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : HttpAdapter {
    
    override suspend fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        return try {
            val requestBody = mapOf("events" to events)
            
            val response = webClient.post()
                .uri(endpoint)
                .headers { httpHeaders ->
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String::class.java)
                .awaitSingle()
            
            HttpResponse(
                ok = response.statusCode.is2xxSuccessful,
                status = response.statusCode.value(),
                data = response.body
            )
        } catch (e: Exception) {
            HttpResponse(ok = false, status = -1, data = e.message)
        }
    }
}

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

/**
 * SLF4J-based logger adapter for Spring
 */
class Slf4jLoggerAdapter(
    private val logger: Logger = LoggerFactory.getLogger("Ripple"),
    private val logLevel: LogLevel = LogLevel.WARN
) : LoggerAdapter {
    
    override fun debug(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.DEBUG && logger.isDebugEnabled) {
            logger.debug(message, *args)
        }
    }

    override fun info(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.INFO && logger.isInfoEnabled) {
            logger.info(message, *args)
        }
    }

    override fun warn(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.WARN && logger.isWarnEnabled) {
            logger.warn(message, *args)
        }
    }

    override fun error(message: String, vararg args: Any?) {
        if (logLevel <= LogLevel.ERROR && logger.isErrorEnabled) {
            logger.error(message, *args)
        }
    }
}
