package codebase.scenarios

import codebase.koog.agentic.AgenticChunkRepository
import codebase.koog.agentic.AgenticIngestor
import codebase.koog.agentic.IngestionReport

class AgenticIngestorWorld {

    companion object {
        private var sharedContainer: org.testcontainers.containers.PostgreSQLContainer<Nothing>? = null
        private var sharedConnectionFactory: io.r2dbc.spi.ConnectionFactory? = null
        private var sharedRepository: AgenticChunkRepository? = null

        @Synchronized
        fun ensureStarted() {
            if (sharedContainer == null || !sharedContainer!!.isRunning) {
                val container = org.testcontainers.containers.PostgreSQLContainer<Nothing>("pgvector/pgvector:pg17").apply {
                    withDatabaseName("codebase_ingestor_cucumber")
                    withUsername("codebase")
                    withPassword("codebase")
                    withReuse(false)
                }
                container.start()
                sharedContainer = container

                val config = io.r2dbc.postgresql.PostgresqlConnectionConfiguration.builder()
                    .host(container.host)
                    .port(container.getMappedPort(5432))
                    .database(container.databaseName)
                    .username(container.username)
                    .password(container.password)
                    .build()
                sharedConnectionFactory = io.r2dbc.postgresql.PostgresqlConnectionFactory(config)
                sharedRepository = AgenticChunkRepository(sharedConnectionFactory!!)
            }
        }
    }

    val connectionFactory: io.r2dbc.spi.ConnectionFactory
        get() {
            ensureStarted()
            return sharedConnectionFactory!!
        }

    val repository: AgenticChunkRepository
        get() {
            ensureStarted()
            return sharedRepository!!
        }

    val ingestor: AgenticIngestor
        get() = AgenticIngestor(repository = repository)

    var lastReport: IngestionReport? = null
    var filesToIngest: MutableList<Pair<String, String>> = mutableListOf()

    fun reset() {
        lastReport = null
        filesToIngest.clear()
    }
}
