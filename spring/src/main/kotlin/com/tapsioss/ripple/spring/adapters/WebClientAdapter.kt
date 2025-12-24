package com.tapsioss.ripple.spring.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration

/**
 * WebClient-based HTTP adapter for Spring applications.
 * 
 * Uses Spring WebFlux's WebClient for HTTP operations. The reactive
 * calls are blocked internally to provide a synchronous interface.
 * 
 * @param webClient WebClient instance. Uses a default client if not provided.
 * @param objectMapper Jackson ObjectMapper for JSON serialization.
 * @param timeout Request timeout duration (default: 30 seconds)
 */
class WebClientAdapter(
    private val webClient: WebClient = WebClient.create(),
    private val objectMapper: ObjectMapper = ObjectMapper(),
    private val timeout: Duration = Duration.ofSeconds(30)
) : HttpAdapter {
    
    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        return try {
            val requestBody = mapOf("events" to events)
            
            val response = webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .headers { httpHeaders ->
                    headers.forEach { (key, value) ->
                        httpHeaders.add(key, value)
                    }
                }
                .bodyValue(requestBody)
                .retrieve()
                .toEntity(String::class.java)
                .block(timeout)
            
            HttpResponse(
                ok = response?.statusCode?.is2xxSuccessful == true,
                status = response?.statusCode?.value() ?: -1,
                data = response?.body
            )
        } catch (e: Exception) {
            HttpResponse(
                ok = false, 
                status = -1, 
                data = e.message
            )
        }
    }
}
