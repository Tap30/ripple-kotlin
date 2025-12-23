package com.tapsioss.ripple.spring.adapters

import com.fasterxml.jackson.databind.ObjectMapper
import com.tapsioss.ripple.core.Event
import com.tapsioss.ripple.core.HttpResponse
import com.tapsioss.ripple.core.adapters.HttpAdapter
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.web.reactive.function.client.WebClient

/**
 * WebClient-based HTTP adapter for Spring
 */
class WebClientAdapter(
    private val webClient: WebClient = WebClient.create(),
    private val objectMapper: ObjectMapper = ObjectMapper()
) : HttpAdapter {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    override fun send(
        endpoint: String,
        events: List<Event>,
        headers: Map<String, String>,
        apiKeyHeader: String
    ): HttpResponse {
        return runBlocking {
            scope.async {
                try {
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
            }.await()
        }
    }
}
