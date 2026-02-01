package com.tapsioss.ripple.testserver

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import java.util.concurrent.CopyOnWriteArrayList

val events = CopyOnWriteArrayList<JsonObject>()
var returnError = false

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) { json() }
        routing {
            post("/events") {
                if (returnError) {
                    call.respond(HttpStatusCode.InternalServerError, "Simulated error")
                    return@post
                }
                val body = call.receiveText()
                val json = Json.parseToJsonElement(body)
                when (json) {
                    is JsonArray -> json.forEach { events.add(it.jsonObject) }
                    is JsonObject -> events.add(json)
                    else -> {}
                }
                call.respond(HttpStatusCode.OK, mapOf("received" to events.size))
            }

            get("/events") {
                call.respond(events)
            }

            delete("/events") {
                events.clear()
                call.respond(HttpStatusCode.OK)
            }

            post("/config") {
                val config = call.receive<JsonObject>()
                returnError = config["error"]?.jsonPrimitive?.boolean ?: false
                call.respond(HttpStatusCode.OK)
            }
        }
    }.start(wait = true)
}
