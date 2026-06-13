package codebase.scenarios

import codebase.koog.agentic.AgenticChunker
import codebase.koog.agentic.AgenticCompiler
import codebase.koog.agentic.AgenticOntologizer
import codebase.koog.agentic.CompiledArtifact
import codebase.koog.agentic.OntologizedChunk
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgenticChunkEnforcementSteps(private val world: AgenticChunkEnforcementWorld) {

    @Before("@epic_y_7")
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
    }

    @Given("the agentic schema is initialized for enforcement")
    fun `agentic schema initialized`() = runBlocking {
        world.repository.initSchema()
    }

    @Given("an AGENT.adoc file with a rule {string}")
    fun `agent adoc file with rule`(rule: String) {
        world.filesToIngest.add("AGENT.adoc" to """
            = AGENT.adoc — Directives Agent
            :date: 2026-06-13

            == Regles Absolues

            $rule
        """.trimIndent())
    }

    @Given("an AGENT.adoc file with multiple rules forbidding commit, merge, and push")
    fun `agent adoc file with multiple rules`() {
        world.filesToIngest.add("AGENT.adoc" to """
            = AGENT.adoc — Directives Agent
            :date: 2026-06-13

            == Regles Absolues

            INTERDICTION FORMELLE de commit sans permission explicite.

            INTERDICTION FORMELLE de merge sans permission explicite.

            INTERDICTION FORMELLE de git push sans flag --dry-run.
        """.trimIndent())
    }

    @When("I ingest the files and register enforcement rules")
    fun `ingest and register enforcement`() = runBlocking {
        world.lastReport = world.ingestor.ingest(world.filesToIngest)

        val chunks = world.repository.listChunks(Int.MAX_VALUE)
        val chunker = AgenticChunker()
        val ontologizer = AgenticOntologizer()
        val compiler = AgenticCompiler()

        val artifacts = mutableListOf<CompiledArtifact>()
        for (chunk in chunks) {
            val artifact = compiler.compile(chunk)
            if (artifact != null) artifacts.add(artifact)
        }

        world.enforcement.registerFromCompiled(artifacts, chunks)
    }

    @Then("the enforcement blocks {string} with command {string}")
    fun `enforcement blocks tool with command`(toolName: String, command: String) {
        val result = world.enforcement.check(toolName, mapOf("command" to command))
        world.lastEnforcementResult = result
        assertFalse(result.allowed, "Expected $toolName '$command' to be blocked")
        assertNotNull(result.blockedBy, "BlockedBy should not be null")
        assertNotNull(result.reason, "Reason should not be null")
    }

    @Then("the enforcement blocks {string} with task {string}")
    fun `enforcement blocks tool with task`(toolName: String, task: String) {
        val result = world.enforcement.check(toolName, mapOf("task" to task))
        world.lastEnforcementResult = result
        assertFalse(result.allowed, "Expected $toolName '$task' to be blocked")
        assertNotNull(result.blockedBy, "BlockedBy should not be null")
        assertNotNull(result.reason, "Reason should not be null")
    }

    @Then("the enforcement allows {string} with command {string}")
    fun `enforcement allows tool with command`(toolName: String, command: String) {
        val result = world.enforcement.check(toolName, mapOf("command" to command))
        world.lastEnforcementResult = result
        assertTrue(result.allowed, "Expected $toolName '$command' to be allowed")
    }

    @Then("the enforcement allows {string} with task {string}")
    fun `enforcement allows tool with task`(toolName: String, task: String) {
        val result = world.enforcement.check(toolName, mapOf("task" to task))
        world.lastEnforcementResult = result
        assertTrue(result.allowed, "Expected $toolName '$task' to be allowed")
    }
}
