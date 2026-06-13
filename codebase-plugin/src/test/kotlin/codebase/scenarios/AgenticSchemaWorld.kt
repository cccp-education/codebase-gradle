package codebase.scenarios

import codebase.koog.agentic.AgenticChunkRepository
import codebase.koog.agentic.OntologizedChunk

class AgenticSchemaWorld {

    companion object {
        private var sharedContainer: org.testcontainers.containers.PostgreSQLContainer<Nothing>? = null
        private var sharedConnectionFactory: io.r2dbc.spi.ConnectionFactory? = null
        private var sharedRepository: AgenticChunkRepository? = null

        @Synchronized
        fun ensureStarted() {
            if (sharedContainer == null || !sharedContainer!!.isRunning) {
                val container = org.testcontainers.containers.PostgreSQLContainer<Nothing>("pgvector/pgvector:pg17").apply {
                    withDatabaseName("codebase_agentic_cucumber")
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
