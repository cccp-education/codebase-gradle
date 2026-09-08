package codebase.store

import kotlinx.serialization.Serializable

/**
 * A [DocumentChunk] paired with its doubt metadata at ingestion time
 * (EPIC OCR-QUALITY US-4).
 *
 * The doubt rides with the chunk — produced upstream by the OCR engine
 * (codex N2), transported by the RAG ingestion (codebase N1).
 *
 * @property chunk the semantic chunk to ingest
 * @property doubt the doubt metadata (confidence + doubtful flag)
 */
@Serializable
data class DoubtfulChunk(
    val chunk: DocumentChunk,
    val doubt: DoubtMetadata = DoubtMetadata()
)