package codebase.rag

import codebase.store.RetrieveResult
import java.util.Locale

/**
 * Pure kernel of the composite context expositions (EPIC CB-PAGE-PROVENANCE US-3, D8).
 *
 * Maps a [RetrieveResult] into the two downstream formats that expose it:
 *  - the codex JSON entries of [CodebaseCompositeContextTask];
 *  - the Docs channel meta line of [CompositeContextBuilder.loadDocsContext].
 *
 * Page provenance is **additive** — `pages` is rendered only when
 * non-empty, so a chunk without page provenance produces a line / entry
 * byte-identical to the pre-EPIC format (backward compat total).
 */
object CompositeContextEntries {

    /**
     * JSON entry shape for [CodebaseCompositeContextTask]. Gains the `pages`
     * field when the chunk carries page provenance (additive, omitted when empty).
     */
    fun codexEntry(r: RetrieveResult): Map<String, Any> {
        val entry = mutableMapOf<String, Any>(
            "source" to "codex",
            "chunkId" to r.chunkId,
            "chunkText" to r.chunkText.take(500),
            "sectionPath" to r.sectionPath,
            "headingLevel" to r.headingLevel,
            "sourceDocument" to r.sourceDocument,
            "similarity" to r.similarity
        )
        if (r.pages.isNotEmpty()) {
            entry["pages"] = r.pages
        }
        return entry
    }

    /**
     * Docs channel meta line. Gains ` pages=[40,41]` when the chunk carries
     * page provenance (additive, omitted when empty — identical to the
     * legacy format otherwise).
     */
    fun docsMetaLine(r: RetrieveResult): String {
        val base = "[Doc] source=${r.sourceDocument} section=${r.sectionPath} sim=${"%.3f".format(Locale.US, r.similarity)}"
        return if (r.pages.isNotEmpty()) "$base pages=[${r.pages.joinToString(",")}]" else base
    }
}