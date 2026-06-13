package codebase.koog.agentic

class AgenticExternalImporter(
    private val ingestor: AgenticIngestor
) {

    suspend fun import(externalSystem: String, sourceName: String, content: String): IngestionReport {
        if (content.isBlank()) return IngestionReport(0, 0, 0, 0, 0)

        val normalizedContent = normalizeContent(externalSystem, sourceName, content)
        val sourceFile = "$externalSystem:$sourceName"

        return ingestor.ingest(listOf(sourceFile to normalizedContent))
    }

    private fun normalizeContent(externalSystem: String, sourceName: String, content: String): String {
        val header = buildHeader(externalSystem, sourceName)
        val normalized = formatForChunking(content)

        return """
            |$header
            |
            |$normalized
        """.trimMargin()
    }

    private fun buildHeader(externalSystem: String, sourceName: String): String {
        return "= External Agentic Literature — $externalSystem : $sourceName"
    }

    private fun formatForChunking(content: String): String {
        val lines = content.lines()
        val result = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("# ") && !trimmed.startsWith("## ")) {
                result.add("== ${trimmed.removePrefix("# ").trim()}")
            } else if (trimmed.startsWith("## ") && !trimmed.startsWith("### ")) {
                result.add("== ${trimmed.removePrefix("## ").trim()}")
            } else if (trimmed.startsWith("### ")) {
                result.add("=== ${trimmed.removePrefix("### ").trim()}")
            } else if (trimmed.startsWith("---") && trimmed == "---") {
                result.add("")
            } else if (trimmed.startsWith("name:") || trimmed.startsWith("version:") ||
                trimmed.startsWith("rules:") || trimmed.startsWith("description:")) {
                result.add(":$trimmed")
            } else if (trimmed == "---") {
                result.add("")
            } else {
                result.add(line)
            }
        }

        return result.joinToString("\n")
    }
}
