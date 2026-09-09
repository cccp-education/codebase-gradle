package codebase.store

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads a codex chunks.json corpus file into typed [DocumentChunk]s
 * (EPIC CB-FPA-RAG US-1).
 *
 * The codex corpus format is the [DocumentChunk] serialization —
 * compatibility confirmed by the US-0 audit (S-213). Pure loader: no
 * state, no network, single responsibility (file → typed chunks).
 */
object CorpusChunksLoader {

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses the chunks file (JSON array of [DocumentChunk]). */
    fun load(file: File): List<DocumentChunk> =
        json.decodeFromString<List<DocumentChunk>>(file.readText())
}