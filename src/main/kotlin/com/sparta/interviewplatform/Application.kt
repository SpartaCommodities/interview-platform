package com.sparta.interviewplatform

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("com.sparta.interviewplatform.Application")

fun main() {
    val config = try {
        AppConfig.fromEnvironment()
    } catch (e: IllegalStateException) {
        // a stack trace here tells the operator nothing useful. the message names
        // exactly which variables are wrong.
        System.err.println("configuration error: ${e.message}")
        System.err.println("required: DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD. optional: PORT")
        kotlin.system.exitProcess(1)
    }

    val database = Database(config)
    val repository = JdbcPortRepository(database.dataSource)

    Runtime.getRuntime().addShutdownHook(Thread { database.close() })

    log.info("starting on port {}", config.port)
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        module(repository, database)
    }.start(wait = true)
}
