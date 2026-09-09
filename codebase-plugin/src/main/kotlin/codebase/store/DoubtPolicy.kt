package codebase.store

/**
 * Derives [DoubtMetadata] from corpus text markers (EPIC CB-FPA-RAG US-1).
 *
 * The codex chunk corpus (chunks.json) carries no confidence column — the
 * doubt is derived heuristically from the visible OCR markers. A chunk
 * whose text (content, sectionPath or overlapNext) carries the
 * `[ILLISIBLE]` marker is ingested with zero confidence and flagged
 * doubtful; every other chunk keeps the default full confidence.
 *
 * Pure policy object — no I/O, no state, unit-testable in isolation.
 */
object DoubtPolicy {

    /** Stable OCR marker documented in `book-ocr-issues.json`. */
    const val ILLISIBLE_MARKER = "[ILLISIBLE]"

    /** Doubtful chunks carry zero confidence (worst-case OCR page). */
    const val ILLISIBLE_CONFIDENCE = 0.0

    /** Derives the doubt metadata carried by a single chunk. */
    fun derive(chunk: DocumentChunk): DoubtMetadata =
        if (isDoubtful(chunk)) {
            DoubtMetadata(confidence = ILLISIBLE_CONFIDENCE, doubtful = true)
        } else {
            DoubtMetadata()
        }

    /** Wraps every chunk with its derived doubt metadata. */
    fun mark(chunks: List<DocumentChunk>): List<DoubtfulChunk> =
        chunks.map { DoubtfulChunk(chunk = it, doubt = derive(it)) }

    private fun isDoubtful(chunk: DocumentChunk): Boolean =
        chunk.content.contains(ILLISIBLE_MARKER) ||
            chunk.sectionPath.contains(ILLISIBLE_MARKER) ||
            (chunk.overlapNext?.contains(ILLISIBLE_MARKER) ?: false)
}