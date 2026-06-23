package codebase.koog.agentic

import codebase.koog.session.MigrationRunner
import io.r2dbc.spi.Connection
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Implémentation R2DBC du port [InvalidChunkRepository].
 * Persiste les chunks invalides dans la table `invalid_chunks`.
 */
class R2dbcInvalidChunkRepository(
    private val connectionFactory: ConnectionFactory
) : InvalidChunkRepository {

    override suspend fun initSchema() {
        MigrationRunner(connectionFactory).migrate()
    }

    private suspend fun <R> withConnection(block: suspend (Connection) -> R): R {
        val conn = Mono.from(connectionFactory.create()).awaitSingle()
        try {
            return block(conn)
        } finally {
            Mono.from(conn.close()).subscribe()
        }
    }

    override suspend fun insertQuarantine(chunk: AgenticChunk, errors: List<ChunkValidationError>): Boolean {
        return withConnection { conn ->
            val statement = conn.createStatement(
                "INSERT INTO invalid_chunks (id, source_file, source_lines, content, errors, quarantined_at)" +
                " VALUES ($1, $2, $3, $4, $5::jsonb, $6)" +
                " ON CONFLICT (id) DO NOTHING"
            )
            statement.bind("$1", chunk.id)
            statement.bind("$2", chunk.sourceFile)
            statement.bind("$3", chunk.sourceLines)
            statement.bind("$4", chunk.content)
            statement.bind("$5", Json.encodeToString(errors.map { it.toJsonModel() }))
            statement.bind("$6", Instant.now())

            val updated = Mono.from(statement.execute())
                .flatMap { Mono.from(it.rowsUpdated) }
                .defaultIfEmpty(0L)
                .awaitSingle()
            updated > 0L
        }
    }

    override suspend fun listQuarantined(limit: Int): List<InvalidChunk> {
        return withConnection { conn ->
            val statement = conn.createStatement(
                "SELECT id, source_file, source_lines, content, errors, quarantined_at" +
                " FROM invalid_chunks ORDER BY quarantined_at DESC LIMIT $1"
            )
            statement.bind("$1", limit)

            Mono.from(statement.execute())
                .flatMapMany { result ->
                    result.map { row, _ ->
                        val errorsJson = row.get("errors", String::class.java) ?: "[]"
                        val errorsJsonList: List<ChunkValidationErrorJson> = Json.decodeFromString(errorsJson)
                        InvalidChunk(
                            id = row.get("id", String::class.java)!!,
                            sourceFile = row.get("source_file", String::class.java)!!,
                            sourceLines = row.get("source_lines", String::class.java)!!,
                            content = row.get("content", String::class.java)!!,
                            errors = errorsJsonList.map { it.toDomainModel() },
                            quarantinedAt = row.get("quarantined_at", Instant::class.java)!!
                        )
                    }
                }
                .collectList()
                .awaitSingle()
        }
    }

    override suspend fun countQuarantined(): Int {
        return withConnection { conn ->
            Mono.from(
                conn.createStatement("SELECT count(*) FROM invalid_chunks").execute()
            ).flatMap { result ->
                Mono.from(result.map { row, _ -> row.get(0, Long::class.java)!! })
            }.awaitSingle().toInt()
        }
    }

    private fun ChunkValidationError.toJsonModel(): ChunkValidationErrorJson =
        ChunkValidationErrorJson(
            sourceFile = sourceFile,
            sourceLines = sourceLines,
            lineStart = lineStart,
            lineEnd = lineEnd,
            errorType = errorType.name,
            message = message
        )

    private fun ChunkValidationErrorJson.toDomainModel(): ChunkValidationError =
        ChunkValidationError(
            sourceFile = sourceFile,
            sourceLines = sourceLines,
            lineStart = lineStart,
            lineEnd = lineEnd,
            errorType = ChunkValidationErrorType.valueOf(errorType),
            message = message
        )

    @kotlinx.serialization.Serializable
    private data class ChunkValidationErrorJson(
        val sourceFile: String,
        val sourceLines: String,
        val lineStart: Int?,
        val lineEnd: Int?,
        val errorType: String,
        val message: String
    )
}
