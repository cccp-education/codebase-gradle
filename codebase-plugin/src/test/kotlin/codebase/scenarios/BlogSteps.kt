package codebase.scenarios

import codebase.blog.BlogArticleData
import codebase.blog.BlogArticleExtractor
import codebase.blog.BlogArticleRenderer
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BlogSteps(private val world: BlogWorld) {

    @Before("@epic12")
    fun before() {
        world.reset()
    }

    @After("@epic12")
    fun after() {
        world.tmpDir?.let { it.deleteRecursively() }
    }

    @Given("un repertoire temporaire avec des sessions de test dans foundry")
    fun `temp dir with test sessions in foundry`() {
        world.tmpDir = Files.createTempDirectory("blog-epic12-test").toFile()
        val foundryDir = File(world.tmpDir, "foundry")
        foundryDir.mkdirs()
    }

    @Given("1 session dans le dossier {string}")
    fun `session in path`(relativePath: String) {
        val foundryDir = File(world.tmpDir, "foundry")
        val sessionFile = File(foundryDir, "$relativePath/042-test.adoc")
        sessionFile.parentFile.mkdirs()
        sessionFile.writeText("""
= Session 042 — EPIC 7 Pipeline LLM — TDD complet
:docdate: 2026-06-15

== Contexte
Projet test-borough — N2 pipeline LLM.

== Realise
=== EPIC 7.1 — PromptTemplateFactory
- 5 templates (CDA, FPA, default, system, user)
- 3/3 JUnit5 PASS

== Tests
- 7/7 JUnit5 PASS (PromptTemplateFactoryTest + LlmPipelineOrchestratorTest)

== Prochaine Session
- EPIC 7.3 — Pipeline error recovery
""".trimIndent(), Charsets.UTF_8)
    }

    @When("le BlogArticleExtractor extrait les sessions")
    fun `extract sessions`() {
        val foundryDir = File(world.tmpDir, "foundry")
        val extractor = BlogArticleExtractor()
        world.extractedArticles = extractor.extract(foundryDir)
    }

    @Then("1 article est extrait")
    fun `1 article extracted`() {
        assertEquals(1, world.extractedArticles.size)
    }

    @Then("l'article a le titre {string}")
    fun `article has title`(title: String) {
        assertTrue(world.extractedArticles.isNotEmpty())
        assertEquals(title, world.extractedArticles[0].sessionTitle)
    }

    @Then("l'article vient du borough {string}")
    fun `article from borough`(borough: String) {
        assertTrue(world.extractedArticles.isNotEmpty())
        assertEquals(borough, world.extractedArticles[0].boroughName)
    }

    @Then("l'article a le numero de session {string}")
    fun `article has session number`(number: String) {
        assertTrue(world.extractedArticles.isNotEmpty())
        assertEquals(number, world.extractedArticles[0].sessionNumber)
    }

    @Given("1 session extraite {string} du borough {string}")
    fun `article extracted manually`(sessionNumber: String, borough: String) {
        world.extractedArticles = listOf(
            BlogArticleData(
                sessionNumber = sessionNumber,
                sessionTitle = "Test Session $sessionNumber",
                sessionDate = "2026-06-15",
                boroughName = borough,
                context = "Contexte de la session de test.",
                achievements = "=== Feature X\nImplementation, 5/5 PASS",
                testResults = "- 5/5 JUnit5 PASS",
                nextSession = "EPIC 2"
            )
        )
    }

    @When("le BlogArticleRenderer génère l'article dans le dossier blog")
    fun `render article to blog dir`() {
        world.blogDir = File(world.tmpDir, "blog-output")
        val renderer = BlogArticleRenderer(articleNumber = 127)
        for (article in world.extractedArticles) {
            renderer.render(article, world.blogDir!!)
        }
        world.generatedFiles = world.blogDir!!.listFiles()?.toList() ?: emptyList()
    }

    @Then("le fichier généré existe")
    fun `generated file exists`() {
        assertTrue(world.generatedFiles.isNotEmpty(), "Expected at least one generated file")
    }

    @Then("le fichier commence par le prefixe {string}")
    fun `file starts with`(prefix: String) {
        val content = world.generatedFiles.first().readText(Charsets.UTF_8)
        assertTrue(content.lines().any { it.startsWith(prefix) }, "Expected line starting with '$prefix'")
    }

    @Then("le fichier contient {string}")
    fun `file contains`(text: String) {
        val content = world.generatedFiles.first().readText(Charsets.UTF_8)
        assertTrue(content.contains(text), "Expected content to contain '$text'")
    }

    @Then("le fichier contient l'en-tête {string}")
    fun `file contains heading`(text: String) {
        val content = world.generatedFiles.first().readText(Charsets.UTF_8)
        assertTrue(content.contains(text), "Expected content to contain heading '$text'")
    }

    @Given("{int} sessions dans {string} boroughs differents")
    fun `sessions in boroughs`(sessionCount: Int, boroughCountStr: String) {
        val boroughCount = boroughCountStr.toInt()
        val foundryDir = File(world.tmpDir, "foundry")
        val boroughs = (1..boroughCount).map { "borough-$it" }
        var sessionNum = 1

        for (borough in boroughs) {
            val visDir = if (borough.hashCode() % 2 == 0) "public" else "private"
            val sessionsDir = foundryDir.resolve("$visDir/$borough/.agents/sessions")
            sessionsDir.mkdirs()

            val sessionsPerBorough = sessionCount / boroughCount
            for (i in 1..sessionsPerBorough) {
                val num = sessionNum.toString().padStart(3, '0')
                sessionsDir.resolve("${num}-session-$borough.adoc").writeText("""
= Session $num — Test $borough
:docdate: 2026-06-${(10 + i).toString().padStart(2, '0')}

== Contexte
Session $i du borough $borough.

== Realise
=== Implementation $i
Done, ${i * 2}/${i * 2} PASS.

== Tests
- ${i * 2}/${i * 2} JUnit5 PASS

== Prochaine Session
US-${i + 1}
""".trimIndent(), Charsets.UTF_8)
                sessionNum++
            }
        }
    }

    @When("le pipeline complet extraction et rendu est exécuté")
    fun `full pipeline`() {
        val foundryDir = File(world.tmpDir, "foundry")
        world.blogDir = File(world.tmpDir, "blog-output")

        val extractor = BlogArticleExtractor()
        world.extractedArticles = extractor.extract(foundryDir)

        var number = 127
        for (article in world.extractedArticles) {
            val renderer = BlogArticleRenderer(articleNumber = number)
            renderer.render(article, world.blogDir!!)
            number++
        }

        world.generatedFiles = world.blogDir!!.listFiles()?.toList() ?: emptyList()
    }

    @Then("{string} articles sont générés")
    fun `articles generated`(countStr: String) {
        assertEquals(countStr.toInt(), world.generatedFiles.size)
    }

    @Then("chaque article a un nom unique avec prefixe numerique de 4 chiffres")
    fun `each article has unique 4-digit prefix`() {
        val names = world.generatedFiles.map { it.name }
        val uniquePrefixes = names.map { it.substring(0, 4) }.toSet()
        assertEquals(names.size, uniquePrefixes.size, "Expected unique prefixes but got duplicates: $names")
        for (name in names) {
            assertTrue(name.substring(0, 4).all { it.isDigit() }, "Expected 4-digit prefix in '$name'")
        }
    }
}
