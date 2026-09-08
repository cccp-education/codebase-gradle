package codebase.store

import java.util.Locale

/**
 * Doubt-aware exposure of retrieved chunks (EPIC OCR-QUALITY US-4).
 *
 * The pure kernel of the augmented-context policy : confident chunks
 * pass verbatim, doubtful chunks are annotated with the marker + their
 * confidence (default) or excluded entirely (opt-in). Consumed by the
 * Docs channel of the composite context so the LLM never cites shaky
 * OCR text unknowingly.
 */
object DoubtExposure {

    /** Stable marker prepended to doubtful chunk lines. */
    const val DOUBT_MARKER = "[DOUBTFUL"

    /**
     * Renders retrieved chunks into context lines, honoring the doubt
     * metadata.
     *
     * @param results the retrieved chunks (similarity-ordered)
     * @param excludeDoubtful when true, doubtful chunks are dropped
     *        instead of annotated (default: false — annotate and keep)
     * @return context lines, one per retained chunk
     */
    fun expose(results: List<RetrieveResult>, excludeDoubtful: Boolean = false): List<String> {
        val retained = if (excludeDoubtful) results.filterNot { it.doubtful } else results
        return retained.map { r ->
            if (r.doubtful) {
                "$DOUBT_MARKER confidence=${"%.2f".format(Locale.US, r.confidence)}] ${r.chunkText.take(500)}"
            } else {
                r.chunkText.take(500)
            }
        }
    }
}