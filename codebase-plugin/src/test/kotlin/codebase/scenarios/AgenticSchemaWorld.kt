package codebase.scenarios

import codebase.infrastructure.PostgresFixture
import codebase.koog.agentic.AgenticChunkRepository
import codebase.koog.agentic.OntologizedChunk

class AgenticSchemaWorld {

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

    var lastInsertedChunkId: String? = null
    var insertedChunkIds: MutableList<String> = mutableListOf()
    var lastRetrievedChunk: OntologizedChunk? = null
    var listedChunks: List<OntologizedChunk> = emptyList()
    var chunkCount: Int = 0
    var relationCount: Int = 0
    var lastRelationId: Long = 0L
    var listedRelations: List<codebase.koog.agentic.ChunkRelation> = emptyList()
    var batchInsertCount: Int = 0
    var batchRelationCount: Int = 0

    fun reset() {
        lastInsertedChunkId = null
        insertedChunkIds.clear()
        lastRetrievedChunk = null
        listedChunks = emptyList()
        chunkCount = 0
        relationCount = 0
        lastRelationId = 0L
        listedRelations = emptyList()
        batchInsertCount = 0
        batchRelationCount = 0
    }
}
