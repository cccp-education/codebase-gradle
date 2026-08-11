package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.llm.FakeLlmProvider
import contracts.agent.Epic
import contracts.agent.Plan
import contracts.agent.GradleTask as PlanTask
import contracts.agent.UserStory
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.vibecoding.registry.ToolRegistry
import codebase.koog.state.VibecodingState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EpicZ6AutofocusSteps(private val world: VibecodingWorld) {

    private var sampleContext: CompositeContext = CompositeContext(
        eagerSection = "",
        ragSection = "",
        graphifySection = "",
        docsSection = "",
        config = CompositeContextConfig()
    )

    private var replanPrompt: String = ""

    @Given("a composite context with 4 channels is prepared")
    fun `prepare composite context`() {
        sampleContext = CompositeContext(
            eagerSection = "AGENT.adoc rules: no commit without permission\nINDEX.adoc: EPIC Z in progress",
            ragSection = "VibecodingGraph.kt:680 lines, koog DSL graph with 5 nodes\nVibecodingState.kt:49 lines, data class with focusLevel field",
            graphifySection = "codebase-plugin → codebase-contracts → workspace-bom\nDAG: N1 → N0",
            docsSection = "AUTOFOCUS_CAPABILITY.adoc: 4 levels BIG_PICTURE/ARCHITECTURE/MODULE/IMPLEMENTATION",
            config = CompositeContextConfig()
        )
    }

    @When("I execute vibecoding with a plan that will fail and maxRetries {int}")
    fun `execute vibecoding with failing plan`(maxRetries: Int) {
        val fakeLlm = FakeLlmProvider()
        fakeLlm.nextResponse = "retry with different approach"
        world.fakeLlmProvider = fakeLlm

        val plan = Plan(
            title = "Fix compilation",
            epics = listOf(
                Epic(name = "E1", description = "test", points = 1, userStories = listOf(
                    UserStory(description = "US1", tasks = listOf(
                        PlanTask(description = "compile", gradleTask = "nonexistentTask")
                    ))
                ))
            ),
            totalPoints = 1,
            estimatedSessions = "1"
        )

        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = fakeLlm
        )

        val state = VibecodingState(
            intention = "fix compilation error in VibecodingGraph.kt:42",
            workspaceRoot = "/tmp",
            maxActions = 5,
            maxRetries = maxRetries,
            plan = plan,
            compositeContext = sampleContext
        )

        world.graph = graph
        world.resultState = graph.execute(state)
        replanPrompt = graph.buildReplanPrompt(world.resultState!!)
    }

    @When("I execute vibecoding with a plan that will fail and maxRetries {int} without composite context")
    fun `execute vibecoding with failing plan no context`(maxRetries: Int) {
        val fakeLlm = FakeLlmProvider()
        fakeLlm.nextResponse = "retry with different approach"
        world.fakeLlmProvider = fakeLlm

        val plan = Plan(
            title = "Fix compilation",
            epics = listOf(
                Epic(name = "E1", description = "test", points = 1, userStories = listOf(
                    UserStory(description = "US1", tasks = listOf(
                        PlanTask(description = "compile", gradleTask = "nonexistentTask")
                    ))
                ))
            ),
            totalPoints = 1,
            estimatedSessions = "1"
        )

        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = fakeLlm
        )

        val state = VibecodingState(
            intention = "fix compilation error",
            workspaceRoot = "/tmp",
            maxActions = 5,
            maxRetries = maxRetries,
            plan = plan,
            compositeContext = null
        )

        world.graph = graph
        world.resultState = graph.execute(state)
    }

    @Then("the vibecoding focus level is {string}")
    fun `focus level is`(expected: String) {
        val state = world.resultState
        assertNotNull(state, "Result state should not be null")
        assertEquals(expected, state.focusLevel, "Focus level should be '$expected' but was '${state.focusLevel}'")
    }

    @Then("the vibecoding zoomed context is not null")
    fun `zoomed context is not null`() {
        val state = world.resultState
        assertNotNull(state, "Result state should not be null")
        assertNotNull(state.zoomedContext, "Zoomed context should not be null")
    }

    @Then("the vibecoding zoomed context eager section is empty")
    fun `zoomed context eager section is empty`() {
        val zc = world.resultState?.zoomedContext
        assertNotNull(zc, "Zoomed context should not be null")
        assertEquals("", zc.eagerSection, "Eager section should be empty for IMPLEMENTATION zoom")
    }

    @Then("the vibecoding zoomed context graphify section is empty")
    fun `zoomed context graphify section is empty`() {
        val zc = world.resultState?.zoomedContext
        assertNotNull(zc, "Zoomed context should not be null")
        assertEquals("", zc.graphifySection, "Graphify section should be empty for IMPLEMENTATION zoom")
    }

    @Then("the vibecoding zoomed context rag section is not empty")
    fun `zoomed context rag section is not empty`() {
        val zc = world.resultState?.zoomedContext
        assertNotNull(zc, "Zoomed context should not be null")
        assertTrue(zc.ragSection.isNotBlank(), "RAG section should not be empty for IMPLEMENTATION zoom")
    }

    @Then("the vibecoding replan prompt contains {string}")
    fun `replan prompt contains`(expected: String) {
        assertTrue(replanPrompt.contains(expected),
            "Replan prompt should contain '$expected' but was: ${replanPrompt.take(500)}")
    }

    @Then("the vibecoding autofocus stack is not empty")
    fun `autofocus stack is not empty`() {
        assertFalse(world.graph.autofocusStack.isEmpty(), "Autofocus stack should not be empty")
    }

    @Then("the vibecoding autofocus stack size is at least {int}")
    fun `autofocus stack size is at least`(minSize: Int) {
        assertTrue(world.graph.autofocusStack.size() >= minSize,
            "Autofocus stack size should be at least $minSize but was ${world.graph.autofocusStack.size()}")
    }
}
