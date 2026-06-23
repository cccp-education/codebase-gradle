package codebase.koog.agentic

data class ChunkDiff(
    val added: List<String>,
    val modified: List<String>,
    val removed: List<String>,
    val unchanged: List<String>
) {

    fun chunksToIngest(): List<String> = added + modified

    fun hasChanges(): Boolean = added.isNotEmpty() || modified.isNotEmpty() || removed.isNotEmpty()
}