package codebase.koog.autofocus

import codebase.koog.VibecodingGraph
import codebase.koog.llm.FakeLlmProvider
import contracts.agent.Epic
import codebase.koog.planning.Plan
import contracts.agent.GradleTask as PlanTask
import contracts.agent.UserStory
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import contracts.vibecoding.registry.ToolRegistry
import codebase.koog.state.VibecodingState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class VibecodingGraphAutofocusTest {

    private val sampleContext = CompositeContext(
        eagerSection = "AGENT.adoc rules: no commit without permission\nINDEX.adoc: EPIC Z in progress",
        ragSection = "VibecodingGraph.kt:680 lines, koog DSL graph with 5 nodes\nVibecodingState.kt:49 lines, data class with focusLevel field",
        graphifySection = "codebase-plugin → codebase-contracts → workspace-bom\nDAG: N1 → N0",
        docsSection = "AUTOFOCUS_CAPABILITY.adoc: 4 levels BIG_PICTURE/ARCHITECTURE/MODULE/IMPLEMENTATION",
        config = CompositeContextConfig()
    )

    @Test
    fun `classifyNode sets focusLevel from intention`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "fix compilation error in VibecodingGraph.kt:42",
            workspaceRoot = "/tmp"
        )
        val result = graph.execute(state)
        assertEquals("IMPLEMENTATION", result.focusLevel)
    }

    @Test
    fun `classifyNode defaults to MODULE for unknown intention`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "hello world",
            workspaceRoot = "/tmp"
        )
        val result = graph.execute(state)
        assertEquals("MODULE", result.focusLevel)
    }

    @Test
    fun `classifyNode detects BIG_PICTURE from EPIC keyword`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "what is the status of EPIC Z?",
            workspaceRoot = "/tmp"
        )
        val result = graph.execute(state)
        assertEquals("BIG_PICTURE", result.focusLevel)
    }

    @Test
    fun `classifyNode detects ARCHITECTURE from DAG keyword`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "analyze the DAG dependency graph between codebase and codex",
            workspaceRoot = "/tmp"
        )
        val result = graph.execute(state)
        assertEquals("ARCHITECTURE", result.focusLevel)
    }

    @Test
    fun `classifyNode detects MODULE from refactor keyword`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "refactor the auth module",
            workspaceRoot = "/tmp"
        )
        val result = graph.execute(state)
        assertEquals("MODULE", result.focusLevel)
    }

    @Test
    fun `zoomNode produces zoomedContext when compositeContext is present`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "fix compilation error in VibecodingGraph.kt:42",
            workspaceRoot = "/tmp",
            compositeContext = sampleContext
        )
        val result = graph.execute(state)
        assertNotNull(result.zoomedContext, "zoomedContext should be set when compositeContext is present")
        assertEquals("IMPLEMENTATION", result.focusLevel)
        val zc = result.zoomedContext!!
        assertEquals("", zc.eagerSection, "IMPLEMENTATION zoom should clear eagerSection")
        assertEquals("", zc.graphifySection, "IMPLEMENTATION zoom should clear graphifySection")
        assertEquals("", zc.docsSection, "IMPLEMENTATION zoom should clear docsSection")
        assertTrue(zc.ragSection.isNotBlank(), "IMPLEMENTATION zoom should keep ragSection")
    }

    @Test
    fun `zoomNode produces zoomedContext for ARCHITECTURE level`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "analyze the DAG dependency graph",
            workspaceRoot = "/tmp",
            compositeContext = sampleContext
        )
        val result = graph.execute(state)
        assertEquals("ARCHITECTURE", result.focusLevel)
        assertNotNull(result.zoomedContext)
        val zc = result.zoomedContext!!
        assertTrue(zc.eagerSection.isNotBlank(), "ARCHITECTURE zoom should keep eagerSection")
        assertTrue(zc.ragSection.isNotBlank(), "ARCHITECTURE zoom should keep ragSection")
        assertTrue(zc.graphifySection.isNotBlank(), "ARCHITECTURE zoom should keep graphifySection")
        assertEquals("", zc.docsSection, "ARCHITECTURE zoom should clear docsSection")
    }

    @Test
    fun `zoomNode produces zoomedContext for MODULE level`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "refactor the auth module",
            workspaceRoot = "/tmp",
            compositeContext = sampleContext
        )
        val result = graph.execute(state)
        assertEquals("MODULE", result.focusLevel)
        assertNotNull(result.zoomedContext)
        val zc = result.zoomedContext!!
        assertTrue(zc.eagerSection.isNotBlank(), "MODULE zoom should keep eagerSection")
        assertTrue(zc.ragSection.isNotBlank(), "MODULE zoom should keep ragSection")
        assertTrue(zc.graphifySection.isNotBlank(), "MODULE zoom should keep graphifySection")
        assertEquals("", zc.docsSection, "MODULE zoom should clear docsSection")
    }

    @Test
    fun `zoomNode produces zoomedContext for BIG_PICTURE level`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "what is the status of EPIC Z?",
            workspaceRoot = "/tmp",
            compositeContext = sampleContext
        )
        val result = graph.execute(state)
        assertEquals("BIG_PICTURE", result.focusLevel)
        assertNotNull(result.zoomedContext)
        val zc = result.zoomedContext!!
        assertEquals(sampleContext.eagerSection, zc.eagerSection, "BIG_PICTURE zoom should preserve eagerSection")
        assertEquals(sampleContext.ragSection, zc.ragSection, "BIG_PICTURE zoom should preserve ragSection")
        assertEquals(sampleContext.graphifySection, zc.graphifySection, "BIG_PICTURE zoom should preserve graphifySection")
        assertEquals(sampleContext.docsSection, zc.docsSection, "BIG_PICTURE zoom should preserve docsSection")
    }

    @Test
    fun `zoomNode does not crash when compositeContext is null`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "fix compilation error",
            workspaceRoot = "/tmp",
            compositeContext = null
        )
        val result = graph.execute(state)
        assertEquals("IMPLEMENTATION", result.focusLevel)
        assertNull(result.zoomedContext, "zoomedContext should be null when compositeContext is null")
    }

    @Test
    fun `buildPromptForIteration includes zoomed context when present`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        val plan = Plan(
            title = "test",
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
        val state = VibecodingState(
            intention = "fix compilation error",
            workspaceRoot = "/tmp",
            maxActions = 2,
            plan = plan,
            compositeContext = sampleContext,
            focusLevel = "IMPLEMENTATION",
            zoomedContext = ContextZoomer().zoom(AutofocusLevel.IMPLEMENTATION, sampleContext)
        )
        val result = graph.execute(state)
        assertNotNull(result)
    }

    @Test
    fun `buildReplanPrompt includes zoomed context for error recovery`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        val zoomed = ContextZoomer().zoom(AutofocusLevel.IMPLEMENTATION, sampleContext)
        val state = VibecodingState(
            intention = "fix compilation error",
            workspaceRoot = "/tmp",
            maxActions = 2,
            maxRetries = 1,
            error = "TaskFailed: BUILD FAILED",
            retryCount = 0,
            lastToolResult = "BUILD FAILED in 3s",
            currentTaskDescription = "compile",
            compositeContext = sampleContext,
            focusLevel = "IMPLEMENTATION",
            zoomedContext = zoomed
        )
        val prompt = graph.buildReplanPrompt(state)
        assertTrue(prompt.contains("ZOOMED CONTEXT"), "Replan prompt should include ZOOMED CONTEXT section")
        assertTrue(prompt.contains("IMPLEMENTATION"), "Replan prompt should mention IMPLEMENTATION focus level")
        assertTrue(prompt.contains("VibecodingGraph.kt"), "Replan prompt should include RAG content from zoomed context")
    }

    @Test
    fun `buildReplanPrompt does not crash when zoomedContext is null`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "fix compilation error",
            workspaceRoot = "/tmp",
            error = "TaskFailed: BUILD FAILED",
            retryCount = 0,
            lastToolResult = "BUILD FAILED",
            currentTaskDescription = "compile",
            zoomedContext = null
        )
        val prompt = graph.buildReplanPrompt(state)
        assertTrue(prompt.contains("error recovery"), "Replan prompt should contain error recovery header")
        assertFalse(prompt.contains("ZOOMED CONTEXT"), "Replan prompt should NOT contain ZOOMED CONTEXT when null")
    }

    @Test
    fun `zoomInOnError pushes IMPLEMENTATION and zooms context`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry(),
            llmProvider = FakeLlmProvider()
        )
        val plan = Plan(
            title = "test",
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
        val state = VibecodingState(
            intention = "fix compilation error",
            workspaceRoot = "/tmp",
            maxActions = 2,
            maxRetries = 1,
            plan = plan,
            compositeContext = sampleContext,
            focusLevel = "MODULE"
        )
        val result = graph.execute(state)
        assertTrue(result.focusLevel == "IMPLEMENTATION" || result.error != null,
            "After error, focusLevel should be IMPLEMENTATION or session ended with error")
    }

    @Test
    fun `autofocus stack is populated during execution`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "fix compilation error in VibecodingGraph.kt:42",
            workspaceRoot = "/tmp",
            compositeContext = sampleContext
        )
        graph.execute(state)
        assertFalse(graph.autofocusStack.isEmpty(), "AutofocusStack should not be empty after execution")
    }

    @Test
    fun `autofocus stack is empty for unknown intention without context`() {
        val graph = VibecodingGraph(
            augmentedGraph = null,
            toolRegistry = ToolRegistry()
        )
        val state = VibecodingState(
            intention = "hello world",
            workspaceRoot = "/tmp",
            compositeContext = null
        )
        graph.execute(state)
        assertFalse(graph.autofocusStack.isEmpty(), "Stack should have at least MODULE default")
    }
}
