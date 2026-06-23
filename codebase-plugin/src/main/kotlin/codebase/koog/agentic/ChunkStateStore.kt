package codebase.koog.agentic

import java.io.File

interface ChunkStateStore {

    fun load(): ChunkSnapshot?

    fun save(snapshot: ChunkSnapshot)

    fun clear()
}

class JsonChunkStateStore(private val stateFile: File) : ChunkStateStore {

    override fun load(): ChunkSnapshot? {
        if (!stateFile.exists()) return null
        val text = stateFile.readText(Charsets.UTF_8).trim()
        if (text.isEmpty() || !text.startsWith("{")) return null
        if (!text.contains("\"entries\"")) return null
        return try {
            parseJson(text)
        } catch (_: Exception) {
            null
        }
    }

    override fun save(snapshot: ChunkSnapshot) {
        stateFile.parentFile?.mkdirs()
        stateFile.writeText(toJson(snapshot), Charsets.UTF_8)
    }

    override fun clear() {
        if (stateFile.exists()) stateFile.delete()
    }

    private fun toJson(snapshot: ChunkSnapshot): String = buildString {
        appendLine("{")
        appendLine("  \"entries\": [")
        snapshot.entries.withIndex().forEach { (index, entry) ->
            append("    { \"id\": \"${escape(entry.id)}\", \"sourceFile\": \"${escape(entry.sourceFile)}\", \"sourceLines\": \"${escape(entry.sourceLines)}\", \"checksum\": \"${escape(entry.checksum)}\" }")
            if (index < snapshot.entries.size - 1) append(",")
            appendLine()
        }
        appendLine("  ]")
        append("}")
    }

    private fun parseJson(text: String): ChunkSnapshot? {
        val entries = mutableListOf<ChunkSnapshotEntry>()
        val idRegex = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
        val sourceFileRegex = Regex("\"sourceFile\"\\s*:\\s*\"([^\"]+)\"")
        val sourceLinesRegex = Regex("\"sourceLines\"\\s*:\\s*\"([^\"]+)\"")
        val checksumRegex = Regex("\"checksum\"\\s*:\\s*\"([^\"]+)\"")
        val objectRegex = Regex("\\{\\s*\"id\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"sourceFile\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"sourceLines\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"checksum\"\\s*:\\s*\"[^\"]+\"\\s*\\}")
        for (match in objectRegex.findAll(text)) {
            val obj = match.value
            val id = idRegex.find(obj)?.groupValues?.get(1) ?: continue
            val sourceFile = sourceFileRegex.find(obj)?.groupValues?.get(1) ?: continue
            val sourceLines = sourceLinesRegex.find(obj)?.groupValues?.get(1) ?: continue
            val checksum = checksumRegex.find(obj)?.groupValues?.get(1) ?: continue
            entries.add(ChunkSnapshotEntry(id, sourceFile, sourceLines, checksum))
        }
        return ChunkSnapshot(entries)
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}