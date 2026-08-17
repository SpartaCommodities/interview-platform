package com.sparta.interviewplatform

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test

private class FakePortRepository(seed: List<Port> = emptyList()) : PortRepository {
    private val stored = seed.associateBy { it.code }.toMutableMap()

    override fun all() = stored.values.sortedBy { it.code }
    override fun byCode(code: String) = stored[code]
    override fun add(port: Port): Port {
        if (stored.containsKey(port.code)) throw DuplicatePortException(port.code)
        stored[port.code] = port
        return port
    }
}

private class FakeHealth(private val readiness: Readiness) : HealthSource {
    override fun readiness() = readiness
}

private val healthy = FakeHealth(Readiness(ready = true, detail = "ok"))

private val rotterdam = Port("NLRTM", "Rotterdam", "Netherlands")

class RoutesTest {
    @Test
    fun `health does not depend on the database`() = testApplication {
        application { module(FakePortRepository(), FakeHealth(Readiness(false, "database down"))) }

        val response = client.get("/health")

        response.status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `ready returns 200 when the database is reachable`() = testApplication {
        application { module(FakePortRepository(), healthy) }

        client.get("/ready").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `ready returns 503 when the database is not reachable`() = testApplication {
        application { module(FakePortRepository(), FakeHealth(Readiness(false, "connection refused"))) }

        val response = client.get("/ready")

        response.status shouldBe HttpStatusCode.ServiceUnavailable
        response.bodyAsText() shouldContain "connection refused"
    }

    @Test
    fun `lists ports`() = testApplication {
        application { module(FakePortRepository(listOf(rotterdam)), healthy) }

        val response = client.get("/api/ports")

        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldContain "Rotterdam"
    }

    @Test
    fun `fetches a port by code`() = testApplication {
        application { module(FakePortRepository(listOf(rotterdam)), healthy) }

        val response = client.get("/api/ports/NLRTM")

        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldContain "Netherlands"
    }

    @Test
    fun `looks up codes case insensitively`() = testApplication {
        application { module(FakePortRepository(listOf(rotterdam)), healthy) }

        client.get("/api/ports/nlrtm").status shouldBe HttpStatusCode.OK
    }

    @Test
    fun `returns 404 for an unknown code`() = testApplication {
        application { module(FakePortRepository(), healthy) }

        client.get("/api/ports/ZZZZZ").status shouldBe HttpStatusCode.NotFound
    }

    @Test
    fun `creates a port`() = testApplication {
        application { module(FakePortRepository(), healthy) }

        val response = client.post("/api/ports") {
            contentType(ContentType.Application.Json)
            setBody("""{"code":"sgsin","name":"Singapore","country":"Singapore"}""")
        }

        response.status shouldBe HttpStatusCode.Created
        response.bodyAsText() shouldContain "SGSIN"
    }

    @Test
    fun `rejects a duplicate port with 409`() = testApplication {
        application { module(FakePortRepository(listOf(rotterdam)), healthy) }

        val response = client.post("/api/ports") {
            contentType(ContentType.Application.Json)
            setBody("""{"code":"NLRTM","name":"Rotterdam","country":"Netherlands"}""")
        }

        response.status shouldBe HttpStatusCode.Conflict
    }

    @Test
    fun `rejects a port with no code`() = testApplication {
        application { module(FakePortRepository(), healthy) }

        val response = client.post("/api/ports") {
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Rotterdam","country":"Netherlands"}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
        response.bodyAsText() shouldContain "code is required"
    }

    @Test
    fun `rejects a port with a short code`() = testApplication {
        application { module(FakePortRepository(), healthy) }

        val response = client.post("/api/ports") {
            contentType(ContentType.Application.Json)
            setBody("""{"code":"AB","name":"Somewhere","country":"Nowhere"}""")
        }

        response.status shouldBe HttpStatusCode.BadRequest
    }
}
