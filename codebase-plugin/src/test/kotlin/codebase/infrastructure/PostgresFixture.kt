package codebase.infrastructure

import org.testcontainers.containers.PostgreSQLContainer

object PostgresFixture {

    val container: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("pgvector/pgvector:pg17").apply {
        withDatabaseName("codebase_test")
        withUsername("codebase")
        withPassword("codebase")
        withStartupTimeout(java.time.Duration.ofMinutes(2))
        withReuse(true)
    }

    init {
        if (!container.isRunning) {
            container.start()
        }
    }

    val jdbcUrl: String get() = container.jdbcUrl
    val username: String get() = container.username
    val password: String get() = container.password
    val host: String get() = container.host
    val port: Int get() = container.getMappedPort(5432)
    val databaseName: String get() = container.databaseName
}
