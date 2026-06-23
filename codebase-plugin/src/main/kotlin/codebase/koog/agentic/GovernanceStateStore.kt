package codebase.koog.agentic

import java.io.File

interface GovernanceStateStore {

    fun load(): GovernanceFileSnapshot?

    fun save(snapshot: GovernanceFileSnapshot)

    fun clear()
}

class JsonGovernanceStateStore(private val stateFile: File) : GovernanceStateStore {

    override fun load(): GovernanceFileSnapshot? {
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

    override fun save(snapshot: GovernanceFileSnapshot) {
        stateFile.parentFile?.mkdirs()
        stateFile.writeText(toJson(snapshot), Charsets.UTF_8)
    }

    override fun clear() {
        if (stateFile.exists()) stateFile.delete()
    }

    private fun toJson(snapshot: GovernanceFileSnapshot): String = buildString {
        appendLine("{")
        appendLine("  \"entries\": [")
        snapshot.entries.withIndex().forEach { (index, entry) ->
            append("    { \"relativePath\": \"${escape(entry.relativePath)}\", \"checksum\": \"${escape(entry.checksum)}\" }")
            if (index < snapshot.entries.size - 1) append(",")
            appendLine()
        }
        appendLine("  ]")
        append("}")
    }

    private fun parseJson(text: String): GovernanceFileSnapshot? {
        val entries = mutableListOf<ScannedFileEntry>()
        val pathRegex = Regex("\"relativePath\"\\s*:\\s*\"([^\"]+)\"")
        val checksumRegex = Regex("\"checksum\"\\s*:\\s*\"([^\"]+)\"")
        val objectRegex = Regex("\\{\\s*\"relativePath\"\\s*:\\s*\"[^\"]+\"\\s*,\\s*\"checksum\"\\s*:\\s*\"[^\"]+\"\\s*\\}")
        for (match in objectRegex.findAll(text)) {
            val obj = match.value
            val path = pathRegex.find(obj)?.groupValues?.get(1) ?: continue
            val checksum = checksumRegex.find(obj)?.groupValues?.get(1) ?: continue
            entries.add(ScannedFileEntry(path, checksum))
        }
        return GovernanceFileSnapshot(entries)
    }

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}