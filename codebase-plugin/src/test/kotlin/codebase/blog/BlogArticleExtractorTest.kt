package codebase.blog

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BlogArticleExtractorTest {

    private val extractor = BlogArticleExtractor()

    @Test
    fun `extract returns empty list when no session files found`(@TempDir tempDir: Path) {
        val foundryDir = Files.createDirectory(tempDir.resolve("foundry"))
        val publicDir = Files.createDirectory(foundryDir.resolve("public"))
        Files.createDirectory(publicDir.resolve("empty-borough"))
        val privateDir = Files.createDirectory(foundryDir.resolve("private"))
        Files.createDirectory(privateDir.resolve("also-empty"))

        val result = extractor.extract(foundryDir.toFile())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `extract parses single session file`(@TempDir tempDir: Path) {
        val foundryDir = tempDir.resolve("foundry")
        val sessionsDir = foundryDir.resolve("public/magic-borough/.agents/sessions")
        Files.createDirectories(sessionsDir)

        val sessionContent = """
= Session 042 — EPIC 7 Pipeline LLM — TDD complet
:docdate: 2026-06-01

== Contexte

Projet magic-borough — N2 pipeline LLM.

== Realise

=== EPIC 7.1 — PromptTemplateFactory

- 5 templates (CDA, FPA, default, system, user)
- 3/3 JUnit5 PASS

=== EPIC 7.2 — LlmPipelineOrchestrator

- execute() → prompt → LLM → parsedOutput
- 4/4 JUnit5 PASS

== Tests

- 7/7 JUnit5 PASS (PromptTemplateFactoryTest + LlmPipelineOrchestratorTest)

== Prochaine Session

- EPIC 7.3 — Pipeline error recovery (retry circuit breaker)
""".trimIndent()

        Files.writeString(sessionsDir.resolve("042-pipeline-llm-tdd.adoc"), sessionContent)

        val result = extractor.extract(foundryDir.toFile())

        assertEquals(1, result.size)
        val article = result[0]
        assertEquals("042", article.sessionNumber)
        assertEquals("EPIC 7 Pipeline LLM — TDD complet", article.sessionTitle)
        assertEquals("2026-06-01", article.sessionDate)
        assertEquals("magic-borough", article.boroughName)
        assertTrue(article.context.contains("Projet magic-borough — N2 pipeline LLM"))
        assertTrue(article.achievements.contains("PromptTemplateFactory"))
        assertTrue(article.achievements.contains("LlmPipelineOrchestrator"))
        assertTrue(article.testResults.contains("7/7 JUnit5 PASS"))
        assertTrue(article.nextSession.contains("EPIC 7.3 — Pipeline error recovery"))
    }

    @Test
    fun `extract returns only sessions from Cercle 4 foundry`(@TempDir tempDir: Path) {
        val foundryDir = tempDir.resolve("foundry")

        val sessionsDir = foundryDir.resolve("public/borough-one/.agents/sessions")
        Files.createDirectories(sessionsDir)
        Files.writeString(sessionsDir.resolve("001-session.adoc"), """
= Session 001 — Test
:docdate: 2026-06-01

== Contexte
Public borough session.
""".trimIndent())

        val result = extractor.extract(foundryDir.toFile())
        assertEquals(1, result.size)
        assertEquals("borough-one", result[0].boroughName)

        val outsideDir = tempDir.resolve("not-foundry/.agents/sessions")
        Files.createDirectories(outsideDir)
        Files.writeString(outsideDir.resolve("002-outside.adoc"), "= Session 002")

        val result2 = extractor.extract(foundryDir.toFile())
        assertEquals(1, result2.size)
    }

    @Test
    fun `extract parses multiple sessions from multiple boroughs`(@TempDir tempDir: Path) {
        val foundryDir = tempDir.resolve("foundry")

        val borough1Sessions = foundryDir.resolve("public/b1/.agents/sessions")
        val borough2Sessions = foundryDir.resolve("private/b2/.agents/sessions")
        Files.createDirectories(borough1Sessions)
        Files.createDirectories(borough2Sessions)

        Files.writeString(borough1Sessions.resolve("001-setup.adoc"), """
= Session 001 — Setup infra
:docdate: 2026-06-10
== Contexte
b1 initialisation.
== Realise
=== Container pgvector
== Tests
2/2 PASS
== Prochaine Session
US-1.2 embeddings
""".trimIndent())

        Files.writeString(borough1Sessions.resolve("002-rag.adoc"), """
= Session 002 — RAG pipeline
:docdate: 2026-06-11
== Contexte
b1 RAG.
== Realise
=== Embedding pipeline
== Tests
5/5 PASS
== Prochaine Session
US-1.3 query
""".trimIndent())

        Files.writeString(borough2Sessions.resolve("001-core.adoc"), """
= Session 001 — Core engine
:docdate: 2026-06-12
== Contexte
b2 engine bootstrap.
== Realise
=== Engine core
== Tests
3/3 PASS
== Prochaine Session
EPIC 2
""".trimIndent())

        val result = extractor.extract(foundryDir.toFile())

        assertEquals(3, result.size)
        val boroughs = result.map { it.boroughName }.distinct().sorted()
        assertEquals(listOf("b1", "b2"), boroughs)
    }

    @Test
    fun `extract skips non-adoc files in sessions directory`(@TempDir tempDir: Path) {
        val foundryDir = tempDir.resolve("foundry")
        val sessionsDir = foundryDir.resolve("public/b1/.agents/sessions")
        Files.createDirectories(sessionsDir)

        Files.writeString(sessionsDir.resolve("001-session.adoc"), """
= Session 001 — Valid
:docdate: 2026-06-01
== Contexte
ok
""".trimIndent())
        Files.writeString(sessionsDir.resolve("readme.txt"), "not an adoc file")
        Files.writeString(sessionsDir.resolve(".gitkeep"), "")

        val result = extractor.extract(foundryDir.toFile())
        assertEquals(1, result.size)
    }

    @Test
    fun `BlogArticleData has all required fields`() {
        val data = BlogArticleData(
            sessionNumber = "042",
            sessionTitle = "EPIC 7 Pipeline LLM",
            sessionDate = "2026-06-01",
            boroughName = "magic-borough",
            context = "Projet magic-borough — N2",
            achievements = "=== EPIC 7.1 — PromptTemplateFactory\n- 5 templates\n- 3/3 PASS",
            testResults = "7/7 JUnit5 PASS",
            nextSession = "EPIC 7.3 — Pipeline error recovery"
        )

        assertEquals("042", data.sessionNumber)
        assertNotNull(data.sessionTitle)
        assertNotNull(data.boroughName)
        assertNotNull(data.context)
        assertNotNull(data.achievements)
    }
}
