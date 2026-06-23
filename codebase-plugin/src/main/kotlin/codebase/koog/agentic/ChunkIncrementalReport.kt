package codebase.koog.agentic

/**
 * Rapport incrémental d'une ingestion de gouvernance au niveau chunk (V-9.20).
 *
 * Décrit les changements détectés entre deux snapshots de chunks : chunks
 * ajoutés, modifiés, supprimés et inchangés. Contrairement à
 * [IncrementalReport] (niveau fichier), ce rapport descend jusqu'au contenu
 * des chunks pour détecter les modifications intra-fichier.
 */
data class ChunkIncrementalReport(
    val chunksAdded: List<String>,
    val chunksModified: List<String>,
    val chunksRemoved: List<String>,
    val chunksUnchanged: List<String>
) {

    val chunksAddedCount: Int get() = chunksAdded.size

    val chunksModifiedCount: Int get() = chunksModified.size

    val chunksRemovedCount: Int get() = chunksRemoved.size

    val chunksUnchangedCount: Int get() = chunksUnchanged.size

    fun hasChanges(): Boolean = chunksAdded.isNotEmpty() || chunksModified.isNotEmpty() || chunksRemoved.isNotEmpty()

    companion object {
        fun from(diff: ChunkDiff): ChunkIncrementalReport = ChunkIncrementalReport(
            chunksAdded = diff.added,
            chunksModified = diff.modified,
            chunksRemoved = diff.removed,
            chunksUnchanged = diff.unchanged
        )
    }
}