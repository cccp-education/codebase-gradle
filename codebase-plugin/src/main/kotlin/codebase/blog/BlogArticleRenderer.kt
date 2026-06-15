package codebase.blog

import java.io.File

class BlogArticleRenderer(val articleNumber: Int) {

    fun render(data: BlogArticleData, outputDir: File) {
        outputDir.mkdirs()

        val articleContent = data.context
        val achievementContent = data.achievements
        val testContent = data.testResults

        val description = articleContent.lines().firstOrNull()?.take(150)?.trim() ?: ""
        val summary = articleContent.lines().firstOrNull()?.take(150)?.trim() ?: ""
        val slug = "session-${data.sessionNumber}-${data.boroughName}-${data.sessionTitle}"
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .replace(Regex("^-+|-+$"), "")
            .take(80)
        val readingTime = "${kotlin.math.max(1, (articleContent.length + achievementContent.length) / 1000)} minutes"

        val formattedNumber = String.format("%04d", articleNumber)
        val filename = "${formattedNumber}_session_${data.sessionNumber}_${data.boroughName}.adoc"

        val file = File(outputDir, filename)

        val content = buildString {
            appendLine("= Session ${data.sessionNumber} — ${data.boroughName} : ${data.sessionTitle}")
            appendLine(":jbake-title: Session ${data.sessionNumber} — ${data.boroughName} : ${data.sessionTitle}")
            appendLine(":jbake-date: ${data.sessionDate}")
            appendLine(":jbake-type: post")
            appendLine(":jbake-status: published")
            appendLine(":jbake-tags: session, ${data.boroughName}, codebase, agent, ia, dev-notes")
            appendLine(":jbake-author: Cheroliv")
            appendLine(":jbake-description: $description")
            appendLine(":jbake-summary: $summary")
            appendLine(":jbake-slug: $slug")
            appendLine(":imagesdir: ./images")
            appendLine(":source-highlighter: highlight.js")
            appendLine(":icons: font")
            appendLine(":idprefix:")
            appendLine(":toc: left")
            appendLine(":toclevels: 3")
            appendLine(":toc-title: Table des matières")
            appendLine(":sectnums:")
            appendLine(":jbake-reading-time: $readingTime")
            appendLine(":tip-caption: 💡")
            appendLine(":note-caption: ℹ️")
            appendLine(":important-caption: ❗")
            appendLine(":caution-caption: 🔒")
            appendLine(":warning-caption: ⚠️")
            appendLine()
            appendLine(".temps de lecture : {jbake-reading-time}")
            appendLine()
            appendLine("[.lead]")
            appendLine(description)
            appendLine()
            appendLine("== Contexte")
            appendLine()
            appendLine(articleContent)
            appendLine()
            if (achievementContent.isNotBlank()) {
                appendLine("== Réalisations")
                appendLine()
                appendLine(achievementContent)
                appendLine()
            } else {
                appendLine("// sections absentes")
                appendLine()
            }
            if (testContent.isNotBlank()) {
                appendLine("== Résultats des Tests")
                appendLine()
                appendLine(testContent)
                appendLine()
            }
            if (data.nextSession.isNotBlank()) {
                appendLine("== Prochaine Session")
                appendLine()
                appendLine(data.nextSession)
                appendLine()
            }
        }

        file.writeText(content, Charsets.UTF_8)
    }
}
