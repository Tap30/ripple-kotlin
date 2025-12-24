package com.tapsioss.ripple.spring.adapters.webflux

import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitExchange
import kotlinx.coroutines.runBlocking
import org.springframework.http.MediaType

/**
 * WebClient-based HTTP adapter for Spring applications.
 * 
 * Provides reactive HTTP functionality using Spring WebFlux WebClient
 * with automatic JSON serialization and error handling.
 * 
 * @param webClient WebClient instance. Uses a default client if not provided.
 */
class WebClientAdapter(
    private val webClient: WebClient = WebClient.builder().build()
) : HttpAdapter {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        return try {
            runBlocking {
                val jsonBody = json.encodeToString(mapOf("events" to events))
                
                val response = webClient.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers { httpHeaders ->
                        headers.forEach { (key, value) ->
                            httpHeaders.add(key, value)
                        }
                    }
                    .bodyValue(jsonBody)
                    .awaitExchange { clientResponse ->
                        HttpResponse(
                            ok = clientResponse.statusCode().is2xxSuccessful,
                            status = clientResponse.statusCode().value(),
                            data = if (clientResponse.statusCode().is2xxSuccessful) {
                                clientResponse.awaitBody<String>()
                            } else {
                                "HTTP ${clientResponse.statusCode().value()}"
                            }
                        )
                    }
                
                response
            }
        } catch (e: Exception) {
            HttpResponse(
                ok = false,
                status = -1,
                data = e.message
            )
        }
    }
}
