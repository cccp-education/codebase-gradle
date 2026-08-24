package codebase.store

import kotlinx.serialization.Serializable

/**
 * Result of a semantic search query against the pgvector store.
 *
 * Verbatim migration of `codex.tasks.RetrieveResult` into `codebase.store`
 * (EPIC CDX-RAG-1, Brooklyn → Queens, N2 → N1). The store now owns its
 * retrieval result type — codebase no longer imports it from codex.
 *
 * @property chunkId primary key of the matching chunk
 * @property chunkIndex position of this chunk within its source document
 * @property chunkText the chunk's full text content
 * @property sectionPath hierarchical section path (e.g. "Chapter 1 > Section 1.2")
 * @property headingLevel heading depth (1-6)
 * @property sourceDocument name of the source document file
 * @property similarity cosine similarity score (0.0 to 1.0)
 */
@Serializable
data class RetrieveResult(
    val chunkId: Long,
    val chunkIndex: Int,
    val chunkText: String,
    val sectionPath: String,
    val headingLevel: Int,
    val sourceDocument: String,
    val similarity: Double
)