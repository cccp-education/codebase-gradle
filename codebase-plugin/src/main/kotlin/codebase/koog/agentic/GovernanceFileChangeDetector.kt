package codebase.koog.agentic

class GovernanceFileChangeDetector {

    fun diff(previous: GovernanceFileSnapshot, current: GovernanceFileSnapshot): GovernanceFileDiff {
        val previousPaths = previous.paths()
        val currentPaths = current.paths()

        val added = (currentPaths - previousPaths).toList()
        val removed = (previousPaths - currentPaths).toList()

        val modified = currentPaths.intersect(previousPaths)
            .filter { path ->
                previous.checksumOf(path) != current.checksumOf(path)
            }
            .toList()

        val unchanged = currentPaths.intersect(previousPaths)
            .filter { path ->
                previous.checksumOf(path) == current.checksumOf(path)
            }
            .toList()

        return GovernanceFileDiff(
            added = added,
            modified = modified,
            removed = removed,
            unchanged = unchanged
        )
    }
}