package com.sparta.interviewplatform

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

data class NewPort(val code: String? = null, val name: String? = null, val country: String? = null)

data class ErrorResponse(val error: String)

fun Application.module(ports: PortRepository, health: HealthSource) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<DuplicatePortException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, ErrorResponse(cause.message ?: "duplicate port"))
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "bad request"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("internal error"))
        }
    }

    routing {
        // liveness: deliberately does not touch the database. the process being up
        // is the only thing this proves.
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }

        // readiness: reports whether the database is reachable and the schema exists.
        get("/ready") {
            val readiness = health.readiness()
            val status = if (readiness.ready) HttpStatusCode.OK else HttpStatusCode.ServiceUnavailable
            call.respond(status, mapOf("ready" to readiness.ready, "detail" to readiness.detail))
        }

        route("/api/ports") {
            get {
                call.respond(ports.all())
            }

            get("/{code}") {
                val code = call.parameters["code"].orEmpty().uppercase()
                val port = ports.byCode(code)
                if (port == null) {
                    call.respond(HttpStatusCode.NotFound, ErrorResponse("no port with code '$code'"))
                } else {
                    call.respond(port)
                }
            }

            post {
                val body = call.receive<NewPort>()
                val port = body.validated()
                call.respond(HttpStatusCode.Created, ports.add(port))
            }
        }
    }
}

private fun NewPort.validated(): Port {
    val code = code?.trim()?.uppercase()
    require(!code.isNullOrEmpty()) { "code is required" }
    require(code.length in 3..10) { "code must be between 3 and 10 characters" }
    require(!name.isNullOrBlank()) { "name is required" }
    require(!country.isNullOrBlank()) { "country is required" }
    return Port(code = code, name = name.trim(), country = country.trim())
}
