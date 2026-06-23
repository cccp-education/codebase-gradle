package codebase.koog.agentic

data class ChunkSnapshotEntry(
    val id: String,
    val sourceFile: String,
    val sourceLines: String,
    val checksum: String
)

data class ChunkSnapshot(
    val entries: List<ChunkSnapshotEntry>
) {

    fun ids(): Set<String> = entries.map { it.id }.toSet()

    fun entryOf(id: String): ChunkSnapshotEntry? = entries.firstOrNull { it.id == id }

    fun checksumOf(id: String): String? = entryOf(id)?.checksum

    companion object {
        fun empty(): ChunkSnapshot = ChunkSnapshot(emptyList())

        fun fromChunks(chunks: List<AgenticChunk>): ChunkSnapshot =
            ChunkSnapshot(chunks.map { ChunkSnapshotEntry(it.id, it.sourceFile, it.sourceLines, it.checksum) })
    }
}