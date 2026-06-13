package codebase.scenarios

import codebase.koog.agentic.AgenticExternalImporter
import codebase.koog.agentic.AgenticIngestor
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import kotlin.test.assertTrue

class AgenticExternalImporterSteps(private val world: AgenticIngestorWorld) {

    private var externalContent: String = ""

    @Before("@epic_y_6")
    fun cleanupDatabase() = runBlocking {
        world.repository.initSchema()
        val conn = Mono.from(world.connectionFactory.create()).awaitSingle()
        try {
            Mono.from(conn.createStatement("TRUNCATE TABLE chunk_relations, agentic_chunks CASCADE").execute())
                .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L)
                .awaitSingle()
        } finally {
            Mono.from(conn.close()).subscribe()
        }
        world.reset()
        externalContent = ""
    }

    @Given("a Copilot rules file with INTERDICTION statements")
    fun `copilot rules with interdiction`() {
        externalContent = """
            # Copilot Rules

            You are an expert Kotlin developer.
            Always write unit tests before implementation.
            INTERDICTION FORMELLE de commit de secrets dans le repository.
            Follow clean architecture principles.

            ## Code Style
            - Use 4 spaces for indentation
            - Max line length: 120 characters
            - Use meaningful variable names
        """.trimIndent()
    }

    @Given("a Cursor rules file with YAML frontmatter")
    fun `cursor rules with yaml`() {
        externalContent = """
            ---
            name: Strict Kotlin Style
            version: 1.0
            rules:
              - Never use !! operator
              - Always handle nullable types safely
              - Prefer data classes for models
              - Max function length: 30 lines
            ---

            # Cursor Instructions

            When generating Kotlin code:
            1. Always add null safety checks
            2. Use extension functions judiciously
            3. Avoid mutable state in data classes
        """.trimIndent()
    }

    @Given("a Claude agent system prompt with constraints")
    fun `claude agent prompt with constraints`() {
        externalContent = """
            System Prompt for Claude Agent:

            You are a senior software engineer working on a Gradle plugin ecosystem.
            Your responsibilities:
            - Write clean, idiomatic Kotlin code
            - Follow TDD methodology strictly
            - Never modify user's personal configuration files
            - Always verify compilation after each change
            - Use 5x20min sessions over 1x2h marathon sessions

            Constraints:
            - Maximum context window: 200k tokens
            - No external API calls without user permission
            - All generated code must compile on first attempt
        """.trimIndent()
    }

    @Given("empty external content")
    fun `empty external content`() {
        externalContent = "   \n  \n   "
    }

    @When("I import the external content as {string}")
    fun `import external content as`(system: String) = runBlocking {
        val importer = AgenticExternalImporter(ingestor = AgenticIngestor(repository = world.repository))
        world.lastReport = importer.import(system, "$system-test-rules", externalContent)
    }

    @When("I import Copilot rules as {string}")
    fun `import copilot rules as`(system: String) = runBlocking {
        val copilotRules = """
            # Copilot Rules
            INTERDICTION de commit de tokens API.
            Write tests before implementation.
        """.trimIndent()
        val importer = AgenticExternalImporter(ingestor = AgenticIngestor(repository = world.repository))
        world.lastReport = importer.import(system, "$system-rules-v1", copilotRules)
    }

    @When("I import Cursor rules as {string}")
    fun `import cursor rules as`(system: String) = runBlocking {
        val cursorRules = """
            # Cursor Rules
            OBLIGATOIRE : verifier la compilation apres chaque modification.
            Max function length: 30 lines.
        """.trimIndent()
        val importer = AgenticExternalImporter(ingestor = AgenticIngestor(repository = world.repository))
        world.lastReport = importer.import(system, "$system-rules-v2", cursorRules)
    }

    @Then("the database contains chunks from external source {string}")
    fun `chunks from external source`(system: String) = runBlocking {
        val chunks = world.repository.listChunks(Int.MAX_VALUE)
        val matching = chunks.filter { it.chunk.sourceFile.startsWith("$system:") }
        assertTrue(matching.isNotEmpty(), "Should have chunks from $system, got ${chunks.map { it.chunk.sourceFile }}")
    }

    @Then("the database contains CONSTRAINT chunks")
    fun `contains constraint chunks`() = runBlocking {
        val chunks = world.repository.listChunks(Int.MAX_VALUE)
        val constraints = chunks.filter { it.chunk.chunkType == codebase.koog.agentic.ChunkType.CONSTRAINT }
        assertTrue(constraints.isNotEmpty(), "Should have CONSTRAINT chunks, got ${chunks.map { it.chunk.chunkType }}")
    }

    @Then("the ingestion report shows artifacts compiled = {int}")
    fun `artifacts compiled equals`(expected: Int) {
        kotlin.test.assertNotNull(world.lastReport)
        kotlin.test.assertEquals(expected, world.lastReport!!.artifactsCompiled)
    }
}
