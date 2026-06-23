package codebase.koog.agentic

class ChunkChangeDetector {

    fun diff(previous: ChunkSnapshot, current: ChunkSnapshot): ChunkDiff {
        val previousIds = previous.ids()
        val currentIds = current.ids()

        val added = (currentIds - previousIds).toList()
        val removed = (previousIds - currentIds).toList()

        val modified = currentIds.intersect(previousIds)
            .filter { id -> previous.checksumOf(id) != current.checksumOf(id) }
            .toList()

        val unchanged = currentIds.intersect(previousIds)
            .filter { id -> previous.checksumOf(id) == current.checksumOf(id) }
            .toList()

        return ChunkDiff(
            added = added,
            modified = modified,
            removed = removed,
            unchanged = unchanged
        )
    }
}