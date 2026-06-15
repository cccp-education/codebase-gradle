package codebase.blog

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EndSessionBlogTaskTest {

    @Test
    fun `task extracts sessions from foundry and renders articles`(@TempDir tempDir: Path) {
        val foundryDir = tempDir.resolve("foundry")
        val sessionsDir = foundryDir.resolve("public/test-borough/.agents/sessions")
        Files.createDirectories(sessionsDir)

        Files.writeString(sessionsDir.resolve("001-session-one.adoc"), """
= Session 001 — Hello World
:docdate: 2026-06-15

== Contexte
First test session with LLM.

== Realise
=== Feature A
Built feature A, 3/3 PASS.

== Tests
- 3/3 JUnit5 PASS

== Prochaine Session
US-2
""".trimIndent())

        val outputDir = tempDir.resolve("blog-output").toFile()

        val blogDir = tempDir.resolve("office/sites/cheroliv/jbake/content/blog/2026")
        Files.createDirectories(blogDir)

        val extractor = BlogArticleExtractor()
        val articles = extractor.extract(foundryDir.toFile())
        assertEquals(1, articles.size)

        val renderer = BlogArticleRenderer(articleNumber = 127)
        renderer.render(articles[0], blogDir.toFile())

        val generatedFiles = blogDir.toFile().listFiles() ?: emptyArray()
        assertEquals(1, generatedFiles.size)
        val content = generatedFiles[0].readText(Charsets.UTF_8)

        assertTrue(content.contains(":jbake-title: Session 001 — test-borough : Hello World"))
        assertTrue(content.contains(":jbake-date: 2026-06-15"))
        assertTrue(content.contains("== Contexte"))
        assertTrue(content.contains("First test session"))
        assertTrue(content.contains("== Réalisations"))
        assertTrue(content.contains("Feature A"))
        assertTrue(content.contains("== Résultats des Tests"))
        assertTrue(content.contains("== Prochaine Session"))
        assertTrue(content.contains("US-2"))
    }

    @Test
    fun `task handles empty foundry directory gracefully`(@TempDir tempDir: Path) {
        val emptyFoundryDir = Files.createDirectory(tempDir.resolve("empty-foundry")).toFile()

        val extractor = BlogArticleExtractor()
        val articles = extractor.extract(emptyFoundryDir)
        assertTrue(articles.isEmpty())
    }

    @Test
    fun `BlogArticleExtractor skips sessions without docdate`(@TempDir tempDir: Path) {
        val foundryDir = tempDir.resolve("foundry")
        val sessionsDir = foundryDir.resolve("public/borough/.agents/sessions")
        Files.createDirectories(sessionsDir)

        Files.writeString(sessionsDir.resolve("001-nodate.adoc"), """
= Session 001 — No Date

== Contexte
Missing docdate.
""".trimIndent())

        Files.writeString(sessionsDir.resolve("002-withdate.adoc"), """
= Session 002 — With Date
:docdate: 2026-06-15

== Contexte
Has docdate.
""".trimIndent())

        val extractor = BlogArticleExtractor()
        val articles = extractor.extract(foundryDir.toFile())
        assertEquals(2, articles.size)
        assertEquals("", articles[0].sessionDate)
        assertEquals("2026-06-15", articles[1].sessionDate)
    }
}
