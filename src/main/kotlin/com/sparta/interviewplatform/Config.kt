package com.sparta.interviewplatform

data class AppConfig(
    val port: Int,
    val databaseUrl: String,
    val databaseUser: String,
    val databasePassword: String,
) {
    companion object {
        /**
         * Builds config from environment variables.
         *
         * Required: DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD.
         * Optional: PORT (defaults to 8080).
         *
         * Throws [IllegalStateException] listing every missing variable, so a
         * misconfigured deployment fails immediately with an actionable message
         * rather than starting and failing on first request.
         */
        fun from(env: Map<String, String>): AppConfig {
            val required = listOf("DATABASE_URL", "DATABASE_USER", "DATABASE_PASSWORD")
            val missing = required.filter { env[it].isNullOrBlank() }
            check(missing.isEmpty()) {
                "missing required environment variables: ${missing.joinToString(", ")}"
            }

            val port = env["PORT"]?.takeIf { it.isNotBlank() } ?: "8080"
            val parsedPort = port.toIntOrNull()
            checkNotNull(parsedPort) { "PORT must be a number, got '$port'" }

            return AppConfig(
                port = parsedPort,
                databaseUrl = env.getValue("DATABASE_URL"),
                databaseUser = env.getValue("DATABASE_USER"),
                databasePassword = env.getValue("DATABASE_PASSWORD"),
            )
        }

        fun fromEnvironment(): AppConfig = from(System.getenv())
    }
}
