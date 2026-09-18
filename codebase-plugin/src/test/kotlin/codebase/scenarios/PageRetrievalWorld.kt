package codebase.scenarios

import codebase.rag.CompositeContextBuilder
import codebase.store.DoubtfulChunk
import codebase.store.RagVectorStore
import codebase.store.RetrieveResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail

/**
 * World for page retrieval scenarios.
 * Uses a fake in-memory RagVectorStore for testing.
 */
class PageRetrievalWorld {
    internal var fakeStore: FakeRagVectorStore = FakeRagVectorStore()
    internal var lastResults: List<RetrieveResult>? = null

    fun storeChunk(content: String, sectionPath: String, headingLevel: Int, sourceDocument: String, pages: List<Int>?) {
        val chunk = DoubtfulChunk(
            chunk = codebase.store.DocumentChunk(
                id = "test-chunk-id",
                sourceDocument = sourceDocument,
                sectionPath = sectionPath,
                headingLevel = headingLevel,
                content = content,
                license = "Apache-2.0"
            ),
            doubt = codebase.store.DoubtMetadata(
                confidence = codebase.store.DoubtMetadata.MAX_CONFIDENCE,
                doubtful = false
            )
        )
        fakeStore.storeChunk(chunk, pages)
    }

    fun search(query: String, topK: Int = 10): List<RetrieveResult> {
        lastResults = runBlocking { fakeStore.search(query, topK) }
        return lastResults ?: emptyList()
    }

    class FakeRagVectorStore : RagVectorStore() {
        private val storedChunks = mutableListOf<Pair<codebase.store.DoubtfulChunk, List<Int>?>>()

        fun storeChunk(chunk: codebase.store.DoubtfulChunk, pages: List<Int>?) {
            storedChunks.add(chunk to pages)
        }

        override suspend fun search(query: String, topK: Int): List<RetrieveResult> {
            // Simple fake: return all stored chunks that contain the query in content (case-insensitive)
            val matches = storedChunks.filter { (chunk, _) ->
                chunk.chunk.content.lowercase().contains(query.lowercase())
            }.map { (chunk, pages) ->
                RetrieveResult(
                    chunkId = chunk.chunk.id.toLong(), // Convert string ID to long for fake
                    chunkIndex = 0,
                    chunkText = chunk.chunk.content,
                    sectionPath = chunk.chunk.sectionPath,
                    headingLevel = chunk.chunk.headingLevel,
                    sourceDocument = chunk.chunk.sourceDocument,
                    similarity = 1.0,
                    confidence = chunk.doubt.confidence,
                    doubtful = chunk.doubt.doubtful,
                    pages = chunk.chunk.pages
                )
            }
            return matches.take(topK)
        }
    }
}