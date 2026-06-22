package codebase.scenarios

import codebase.koog.llm.FakeLlmProvider
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class GovernanceFallbackSteps(private val world: GovernanceFallbackWorld) {

    @Given("a project workspace with AGENT.adoc containing {string}")
    fun `workspace with agent adoc`(content: String) {
        world.newWorkspace()
        world.writeGovernanceFile("AGENT.adoc", "= Agent\n\n$content\n")
    }

    @Given("a project workspace with a governance INDEX file containing {string}")
    fun `workspace with index adoc`(content: String) {
        world.newWorkspace()
        world.writeGovernanceFile(".agents/INDEX.adoc", "= INDEX\n\n$content\n")
    }

    @Given("a project workspace with BACKLOG.adoc containing checkbox items")
    fun `workspace with backlog`() {
        world.newWorkspace()
        world.writeGovernanceFile(
            "BACKLOG.adoc",
            """
            = Backlog

            * [ ] Open item V-LOCAL-4
            * [x] Done item V-LOCAL-2
            * [ ] Other item
            """.trimIndent()
        )
    }

    @Given("an empty project workspace")
    fun `empty workspace`() {
        world.newWorkspace()
    }

    @Given("a multi-module project with AGENT.adoc in subproject {string}")
    fun `multi-module workspace`(subprojectName: String) {
        world.newWorkspace()
        world.writeGovernanceFile("$subprojectName/AGENT.adoc", "= Sub Agent\n\nSubproject agent rules\n")
    }

    @Given("a governance fallback task configured with FakeLlmProvider")
    fun `configure fake llm`() {
        world.llmProvider = FakeLlmProvider()
    }

    @When("I send prompt {string} without contextFile")
    fun `send prompt without context file`(prompt: String) {
        world.executeProtocol(prompt = prompt)
    }

    @Then("the auto-loaded AgentContext eagerRules contains {string}")
    fun `eager rules contains`(expected: String) {
        val ctx = world.agentContext
        assertNotNull(ctx, "AgentContext should be auto-loaded (fallback)")
        assertTrue(
            ctx.eagerRules.contains(expected),
            "Expected '$expected' in eagerRules, got: ${ctx.eagerRules.take(200)}"
        )
    }

    @Then("the auto-loaded AgentContext backlogItems contains {string}")
    fun `backlog items contains`(expected: String) {
        val ctx = world.agentContext
        assertNotNull(ctx, "AgentContext should be auto-loaded (fallback)")
        assertTrue(
            ctx.backlogItems.any { it.contains(expected) },
            "Expected backlog item '$expected', got: ${ctx.backlogItems}"
        )
    }

    @Then("the auto-loaded AgentContext eagerRules is empty")
    fun `eager rules empty`() {
        val ctx = world.agentContext
        assertNotNull(ctx, "AgentContext should be set even when empty")
        assertEquals("", ctx.eagerRules)
    }

    @Then("the governance response status is COMPLETED")
    fun `response completed`() {
        assertNotNull(world.responseContent, "Response content should not be null")
        assertTrue(
            world.responseContent.contains("COMPLETED"),
            "Expected COMPLETED status, got: ${world.responseContent.take(200)}"
        )
    }
}