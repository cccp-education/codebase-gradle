package codebase.scenarios

import codebase.infrastructure.PostgresFixture
import codebase.rag.ChunkTokenizer
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel

object PgVectorTestContext {

    const val DATASETS_DIR = "src/test/resources/datasets"

    fun jdbcUrl() = PostgresFixture.jdbcUrl

    fun jdbcUser() = PostgresFixture.username

    fun jdbcPassword() = PostgresFixture.password

    val model: AllMiniLmL6V2EmbeddingModel by lazy { AllMiniLmL6V2EmbeddingModel() }

    val fileChunks = mutableMapOf<String, List<String>>()

    var topResults = listOf<TopResult>()

    data class TopResult(val chunkId: Long, val text: String, val similarity: Double)

    fun splitIntoSentenceLevelChunks(text: String): List<String> =
        ChunkTokenizer.splitIntoSentenceLevelChunks(text)

    fun estimateTokenCount(text: String): Int =
        ChunkTokenizer.estimateTokenCount(text)
}
