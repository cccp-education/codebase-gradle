package codebase.scenarios

import codebase.infrastructure.PostgresFixture
import codebase.koog.agentic.AgenticChunkEnforcement
import codebase.koog.agentic.AgenticChunkRepository
import codebase.koog.agentic.AgenticIngestor
import codebase.koog.agentic.EnforcementResult
import codebase.koog.agentic.IngestionReport

class AgenticChunkEnforcementWorld {

    companion object {
        private var sharedConnectionFactory: io.r2dbc.spi.ConnectionFactory? = null
        private var sharedRepository: AgenticChunkRepository? = null

        @Synchronized
        fun ensureStarted() {
            if (sharedConnectionFactory == null) {
                val config = io.r2dbc.postgresql.PostgresqlConnectionConfiguration.builder()
                    .host(PostgresFixture.host)
                    .port(PostgresFixture.port)
                    .database(PostgresFixture.databaseName)
                    .username(PostgresFixture.username)
                    .password(PostgresFixture.password)
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

    val enforcement = AgenticChunkEnforcement()

    var lastReport: IngestionReport? = null
    var filesToIngest: MutableList<Pair<String, String>> = mutableListOf()
    var lastEnforcementResult: EnforcementResult? = null

    fun reset() {
        lastReport = null
        filesToIngest.clear()
        lastEnforcementResult = null
        enforcement.clear()
    }
}
