package com.sparta.interviewplatform

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.io.Closeable
import javax.sql.DataSource

data class Readiness(val ready: Boolean, val detail: String)

interface HealthSource {
    fun readiness(): Readiness
}

class Database(config: AppConfig) : HealthSource, Closeable {
    val dataSource: DataSource

    init {
        val hikari = HikariConfig().apply {
            jdbcUrl = config.databaseUrl
            username = config.databaseUser
            password = config.databasePassword
            maximumPoolSize = 5
            // -1 so the pool constructs even when the database is unreachable. the
            // process stays up and reports not-ready, rather than crash-looping
            // before it can serve /health.
            initializationFailTimeout = -1
        }
        dataSource = HikariDataSource(hikari)
    }

    override fun readiness(): Readiness =
        try {
            dataSource.connection.use { connection ->
                connection.createStatement().use { statement ->
                    statement.executeQuery("select count(*) from ports").use { it.next() }
                }
            }
            Readiness(ready = true, detail = "database reachable, schema present")
        } catch (e: Exception) {
            Readiness(ready = false, detail = e.message ?: e::class.simpleName ?: "unknown error")
        }

    override fun close() {
        (dataSource as? HikariDataSource)?.close()
    }
}
