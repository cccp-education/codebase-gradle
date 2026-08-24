package codebase.store

import kotlinx.serialization.Serializable

/**
 * A single semantic chunk extracted from a Markdown/AsciiDoc document.
 *
 * Verbatim migration of `codex.tasks.DocumentChunk` into `codebase.store`
 * (EPIC CDX-RAG-1, Brooklyn → Queens, N2 → N1). The store now owns its
 * chunk type — codebase no longer imports it from codex.
 *
 * @property id deterministic SHA-256 based identifier (e.g. "chk-a1b2c3d4")
 * @property sourceDocument name of the source document file
 * @property sectionPath hierarchical section path (e.g. "Chapter 1 > Section 1.2")
 * @property headingLevel heading depth (1-6)
 * @property content full section content including heading line
 * @property codeBlocks extracted fenced code blocks as strings
 * @property entities named entity references (placeholder for future extraction)
 * @property overlapNext first two sentences of the following section for context continuity
 * @property license license tag for this chunk (Apache-2.0 / PROPRIETARY / UNKNOWN)
 */
@Serializable
data class DocumentChunk(
    val id: String,
    val sourceDocument: String,
    val sectionPath: String,
    val headingLevel: Int,
    val content: String,
    val codeBlocks: List<String> = emptyList(),
    val entities: List<String> = emptyList(),
    val overlapNext: String? = null,
    val license: String = "UNKNOWN"
)