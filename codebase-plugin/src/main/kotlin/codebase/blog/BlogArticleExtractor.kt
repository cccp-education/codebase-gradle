package codebase.blog

import java.io.File

data class BlogArticleData(
    val sessionNumber: String,
    val sessionTitle: String,
    val sessionDate: String,
    val boroughName: String,
    val context: String,
    val achievements: String,
    val testResults: String,
    val nextSession: String
)

class BlogArticleExtractor {

    fun extract(foundryDir: File): List<BlogArticleData> {
        val results = mutableListOf<BlogArticleData>()

        foundryDir.listFiles()?.forEach { visibilityDir ->
            if (!visibilityDir.isDirectory) return@forEach
            visibilityDir.listFiles()?.forEach { boroughDir ->
                if (!boroughDir.isDirectory) return@forEach
                val sessionsDir = File(boroughDir, ".agents/sessions")
                if (!sessionsDir.exists()) return@forEach

                sessionsDir.listFiles()?.forEach { sessionFile ->
                    if (!sessionFile.isFile || !sessionFile.name.endsWith(".adoc")) return@forEach
                    val content = sessionFile.readText(Charsets.UTF_8)
                    val article = parseSessionFile(content, boroughDir.name, sessionFile.name)
                    if (article != null) results.add(article)
                }
            }
        }

        return results.sortedBy { it.sessionNumber }
    }

    private fun parseSessionFile(
        content: String,
        boroughName: String,
        fileName: String
    ): BlogArticleData? {
        val lines = content.lines()
        val titleLine = lines.firstOrNull()?.removePrefix("= ")?.trim() ?: return null
        val sessionNumber = fileName.removeSuffix(".adoc").split("-").firstOrNull()
            ?.takeIf { it.all { c -> c.isDigit() } } ?: "000"
        val sessionTitle = titleLine.substringAfter("— ").trim().ifBlank { titleLine }

        val date = lines.firstOrNull { it.startsWith(":docdate:") }
            ?.removePrefix(":docdate:")?.trim() ?: ""

        val context = extractSection(lines, "Contexte")
        val realisations = extractSection(lines, "Realise")
        val resultats = extractSection(lines, "Test")
        val prochain = extractSection(lines, "Prochaine Session")

        return BlogArticleData(
            sessionNumber = sessionNumber,
            sessionTitle = sessionTitle,
            sessionDate = date,
            boroughName = boroughName,
            context = context,
            achievements = realisations,
            testResults = resultats,
            nextSession = prochain
        )
    }

    private fun extractSection(lines: List<String>, sectionKeyword: String): String {
        val startIdx = lines.indexOfFirst {
            val trimmed = it.trimStart('=', ' ')
            trimmed.startsWith(sectionKeyword, ignoreCase = true)
        }
        if (startIdx < 0) return ""

        var nextSectionIdx = startIdx + 1
        while (nextSectionIdx < lines.size) {
            val line = lines[nextSectionIdx]
            if (line.startsWith("== ") || line == "==") break
            nextSectionIdx++
        }

        return lines.subList(startIdx + 1, nextSectionIdx)
            .joinToString("\n")
            .trim()
    }
}
