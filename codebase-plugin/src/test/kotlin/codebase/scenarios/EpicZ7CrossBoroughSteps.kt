package codebase.scenarios

import codebase.koog.VibecodingGraph
import codebase.koog.autofocus.AutofocusLevel
import codebase.koog.autofocus.ContextZoomer
import codebase.koog.llm.FakeLlmProvider
import contracts.agent.Epic
import vibecoding.contracts.plan.Plan
import contracts.agent.GradleTask as PlanTask
import contracts.agent.UserStory
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.vibecoding.registry.ToolRegistry
import vibecoding.contracts.state.VibecodingState
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class EpicZ7CrossBoroughSteps(private val world: VibecodingWorld) {

    private var sampleContext: CompositeContext = CompositeContext(
        eagerSection = "",
        ragSection = "",
        graphifySection = "",
        docsSection = "",
        config = CompositeContextConfig()
    )

    private var zoomedContextUsed: Boolean = false

    @Given("a composite context with cross-borough references is prepared")
    fun `prepare cross-borough context`() {
        sampleContext = CompositeContext(
            eagerSection = "AGENT.adoc rules: no commit without permission\nINDEX.adoc: EPIC Z in progress",
            ragSection = "VibecodingGraph.kt:718 lines, koog DSL graph with 5 nodes\n" +
                "CodexVectorStore.kt: codex N2 vector store for document retrieval\n" +
                "PlannerIntegration.kt: planner N2 EPIC decomposition bridge",
            graphifySection = "codebase-plugin (N1) → codex-plugin (N2) → planner-plugin (N2) → runner-gradle (N3)\n" +
                "DAG: N1→N2→N3 cross-borough pipeline\n" +
                "codebase-contracts → workspace-bom (N0)",
            docsSection = "AUTOFOCUS_CAPABILITY.adoc: 4 levels BIG_PICTURE/ARCHITECTURE/MODULE/IMPLEMENTATION\n" +
                "PLAN_GLOBAL.adoc: EPIC Z cross-borough validation",
            config = CompositeContextConfig()
        )
    }

    @When("I execute vibecoding with a plan that succeeds and intention {string}")
    fun `execute vibecoding with succeeding plan`(intention: String) {
        val fakeLlm = FakeLlmProvider()
        fakeLlm.nextResponse = "DONE"
        world.fakeLlmProvider = fakeLlm

        val toolRegistry = ToolRegistry()
        toolRegistry.registerHandler("exec_gradle") { _, args, _ ->
            "BUILD SUCCESSFUL in 1s\nTask ${args["task"]} completed"
        }

        val plan = Plan(
            title = "Success plan",
            epics = listOf(
                Epic(name = "E1", description = "test", points = 1, userStories = listOf(
                    UserStory(description = "US1", tasks = listOf(
                        PlanTask(description = "compile", gradleTask = "compileKotlin")
                    ))
                ))
            ),
            totalPoints = 1,
            estimatedSessions = "1"
        )

        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = toolRegistry,
            llmProvider = fakeLlm
        )

        val state = VibecodingState(
            intention = intention,
            workspaceRoot = "/tmp",
            maxActions = 5,
            maxRetries = 1,
            plan = plan,
            compositeContext = sampleContext
        )

        world.graph = graph
        world.resultState = graph.execute(state)
        zoomedContextUsed = world.resultState?.zoomedContext != null
    }

    @When("I execute vibecoding with a plan that fails once then succeeds and intention {string}")
    fun `execute vibecoding with fail then succeed plan`(intention: String) {
        val fakeLlm = FakeLlmProvider()
        fakeLlm.nextResponse = "retry with compileKotlin task"
        world.fakeLlmProvider = fakeLlm

        val toolRegistry = ToolRegistry()
        var callCount = 0
        toolRegistry.registerHandler("exec_gradle") { _, args, _ ->
            callCount++
            if (callCount == 1) {
                "BUILD FAILED in 2s\nCompilation error: unresolved reference"
            } else {
                "BUILD SUCCESSFUL in 1s\nTask ${args["task"]} completed"
            }
        }

        val plan = Plan(
            title = "Fail then succeed",
            epics = listOf(
                Epic(name = "E1", description = "test", points = 1, userStories = listOf(
                    UserStory(description = "US1", tasks = listOf(
                        PlanTask(description = "compile", gradleTask = "compileKotlin")
                    ))
                ))
            ),
            totalPoints = 1,
            estimatedSessions = "1"
        )

        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = toolRegistry,
            llmProvider = fakeLlm
        )

        val state = VibecodingState(
            intention = intention,
            workspaceRoot = "/tmp",
            maxActions = 5,
            maxRetries = 2,
            plan = plan,
            compositeContext = sampleContext
        )

        world.graph = graph
        world.resultState = graph.execute(state)
        zoomedContextUsed = world.resultState?.zoomedContext != null
    }

    @Then("the vibecoding zoomed context graphify section contains {string}")
    fun `zoomed context graphify section contains`(expected: String) {
        val zc = world.resultState?.zoomedContext
        assertNotNull(zc, "Zoomed context should not be null")
        assertTrue(zc.graphifySection.contains(expected),
            "Graphify section should contain '$expected' but was: ${zc.graphifySection.take(300)}")
    }

    @Then("the vibecoding autofocus stack is empty after success pop")
    fun `autofocus stack is empty after success pop`() {
        assertTrue(world.graph.autofocusStack.isEmpty(),
            "Autofocus stack should be empty after success pop but has size ${world.graph.autofocusStack.size()}")
    }

    @Then("the vibecoding result state is finished")
    fun `result state is finished`() {
        val state = world.resultState
        assertNotNull(state, "Result state should not be null")
        assertTrue(state.finished, "Result state should be finished")
    }

    @Then("the vibecoding zoomed context was used during execution")
    fun `zoomed context was used during execution`() {
        assertTrue(zoomedContextUsed, "Zoomed context should have been set during execution")
    }

    @Then("the vibecoding error was recovered")
    fun `error was recovered`() {
        val state = world.resultState
        assertNotNull(state, "Result state should not be null")
        assertTrue(state.finished, "Result state should be finished (error recovered)")
        assertTrue(state.error == null || state.error.contains("Replan"),
            "Error should be null or contain 'Replan' but was: ${state.error}")
    }
}
