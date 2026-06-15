package codebase.blog

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlogArticleRendererTest {

    private val renderer = BlogArticleRenderer(articleNumber = 127)

    @Test
    fun `render produces valid AsciiDoc file with JBake headers`(@TempDir tempDir: Path) {
        val data = BlogArticleData(
            sessionNumber = "042",
            sessionTitle = "EPIC 7 Pipeline LLM — TDD complet",
            sessionDate = "2026-06-01",
            boroughName = "magic-borough",
            context = "Projet magic-borough — N2 pipeline LLM.",
            achievements = "=== EPIC 7.1 — PromptTemplateFactory\n5 templates, 3/3 PASS\n\n=== EPIC 7.2 — LlmPipelineOrchestrator\n4/4 PASS",
            testResults = "- 7/7 JUnit5 PASS",
            nextSession = "EPIC 7.3 — Pipeline error recovery"
        )

        val outputDir = tempDir.resolve("output").toFile()
        renderer.render(data, outputDir)

        val files = outputDir.listFiles() ?: emptyArray()
        assertEquals(1, files.size)
        val outputFile = files[0]
        assertTrue(outputFile.name.startsWith("0127_"))
        assertTrue(outputFile.name.endsWith(".adoc"))

        val content = outputFile.readText(Charsets.UTF_8)
        assertTrue(content.contains(":jbake-title: Session 042 — magic-borough : EPIC 7 Pipeline LLM — TDD complet"))
        assertTrue(content.contains(":jbake-date: 2026-06-01"))
        assertTrue(content.contains(":jbake-type: post"))
        assertTrue(content.contains(":jbake-status: published"))
        assertTrue(content.contains(":jbake-author: Cheroliv"))
        assertTrue(content.contains(":jbake-tags: session,"))
        assertTrue(content.contains(":jbake-description: "))
        assertTrue(content.contains(":jbake-summary: "))
        assertTrue(content.contains(":jbake-slug: "))
        assertTrue(content.contains(":jbake-reading-time: "))
        assertTrue(content.contains("= Session 042 — magic-borough : EPIC 7 Pipeline LLM — TDD complet"))
        assertTrue(content.contains("== Contexte"))
        assertTrue(content.contains("Projet magic-borough"))
        assertTrue(content.contains("== Réalisations"))
        assertTrue(content.contains("EPIC 7.1 — PromptTemplateFactory"))
        assertTrue(content.contains("== Résultats des Tests"))
        assertTrue(content.contains("== Prochaine Session"))
    }

    @Test
    fun `render outputs correct numbering with leading zeros`(@TempDir tempDir: Path) {
        val data = BlogArticleData(
            sessionNumber = "001",
            sessionTitle = "Setup",
            sessionDate = "2026-06-01",
            boroughName = "test",
            context = "init",
            achievements = "done",
            testResults = "OK",
            nextSession = "next"
        )

        val rendererWithNum = BlogArticleRenderer(articleNumber = 8)
        val outputDir = tempDir.resolve("output").toFile()
        rendererWithNum.render(data, outputDir)

        val files = outputDir.listFiles() ?: emptyArray()
        assertEquals(1, files.size)
        assertTrue(files[0].name.startsWith("0008_"))
    }

    @Test
    fun `render creates output directory if not exists`(@TempDir tempDir: Path) {
        val data = BlogArticleData(
            sessionNumber = "001", sessionTitle = "Title", sessionDate = "2026-06-01",
            boroughName = "b", context = "c", achievements = "a", testResults = "t", nextSession = "n"
        )

        val outputDir = File(tempDir.toFile(), "not-yet-created/subdir")
        renderer.render(data, outputDir)

        assertTrue(outputDir.exists())
        assertEquals(1, outputDir.listFiles()?.size ?: 0)
    }

    @Test
    fun `render produces unique filenames for multiple articles`(@TempDir tempDir: Path) {
        val data1 = BlogArticleData(
            sessionNumber = "001", sessionTitle = "First", sessionDate = "2026-06-01",
            boroughName = "b1", context = "c1", achievements = "a1", testResults = "t1", nextSession = "n1"
        )
        val data2 = BlogArticleData(
            sessionNumber = "002", sessionTitle = "Second", sessionDate = "2026-06-02",
            boroughName = "b2", context = "c2", achievements = "a2", testResults = "t2", nextSession = "n2"
        )

        val outputDir = tempDir.resolve("output").toFile()

        val renderer1 = BlogArticleRenderer(articleNumber = 127)
        val renderer2 = BlogArticleRenderer(articleNumber = 128)

        renderer1.render(data1, outputDir)
        renderer2.render(data2, outputDir)

        val files = outputDir.listFiles()?.sortedBy { it.name }?.toList() ?: emptyList()
        assertEquals(2, files.size)
        assertTrue(files[0].name.startsWith("0127_"))
        assertTrue(files[1].name.startsWith("0128_"))
    }

    @Test
    fun `render escape special AsciiDoc characters in content`(@TempDir tempDir: Path) {
        val data = BlogArticleData(
            sessionNumber = "010",
            sessionTitle = "EPIC | Pipeline — TDD",
            sessionDate = "2026-06-15",
            boroughName = "codebase",
            context = "Pipeline | LLM || RAG pgvector",
            achievements = "=== Section | avec pipe",
            testResults = "- 10/10 | PASS",
            nextSession = "EPIC | 11"
        )

        val outputDir = tempDir.resolve("output").toFile()
        renderer.render(data, outputDir)

        val content = outputDir.listFiles()!![0].readText(Charsets.UTF_8)
        assertTrue(content.contains("Session 010 — codebase : EPIC | Pipeline — TDD"))
        assertTrue(content.contains("Pipeline | LLM"))
    }

    @Test
    fun `render handles empty achievements and test results gracefully`(@TempDir tempDir: Path) {
        val data = BlogArticleData(
            sessionNumber = "099",
            sessionTitle = "Empty achievements",
            sessionDate = "2026-06-15",
            boroughName = "test-borough",
            context = "Some context.",
            achievements = "",
            testResults = "",
            nextSession = "No next session"
        )

        val outputDir = tempDir.resolve("output").toFile()
        renderer.render(data, outputDir)

        val content = outputDir.listFiles()!![0].readText(Charsets.UTF_8)
        assertTrue(content.contains("= Session 099"))
        assertTrue(content.contains("Some context."))
        assertTrue(content.contains("// sections absentes"))
    }

    @Test
    fun `BlogArticleRenderer stores articleNumber`() {
        val r = BlogArticleRenderer(articleNumber = 42)
        assertEquals(42, r.articleNumber)
    }
}
