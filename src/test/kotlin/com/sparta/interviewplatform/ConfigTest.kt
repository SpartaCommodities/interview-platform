package com.sparta.interviewplatform

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class ConfigTest {
    private val validEnv = mapOf(
        "DATABASE_URL" to "jdbc:postgresql://localhost:5432/ports",
        "DATABASE_USER" to "ports",
        "DATABASE_PASSWORD" to "secret",
    )

    @Test
    fun `defaults port to 8080 when not set`() {
        AppConfig.from(validEnv).port shouldBe 8080
    }

    @Test
    fun `reads port from the environment`() {
        AppConfig.from(validEnv + ("PORT" to "9999")).port shouldBe 9999
    }

    @Test
    fun `rejects a non numeric port`() {
        val error = shouldThrow<IllegalStateException> {
            AppConfig.from(validEnv + ("PORT" to "eight thousand"))
        }
        error.message!! shouldContain "PORT must be a number"
    }

    @Test
    fun `names every missing required variable`() {
        val error = shouldThrow<IllegalStateException> { AppConfig.from(emptyMap()) }
        error.message!! shouldContain "DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD"
    }

    @Test
    fun `treats a blank variable as missing`() {
        val error = shouldThrow<IllegalStateException> {
            AppConfig.from(validEnv + ("DATABASE_PASSWORD" to "  "))
        }
        error.message!! shouldContain "DATABASE_PASSWORD"
    }
}
