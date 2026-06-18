package codebase.koog

import contracts.session.AgentContext
import contracts.vibecoding.registry.AuditEntry
import codebase.koog.state.VibecodingState
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class LiveContextInjectorTest {

    private val injector = LiveContextInjector()

    @Test
    fun `injectLiveContext always includes live section with metadata`() {
        val state = VibecodingState(
            intention = "test",
            workspaceRoot = "/tmp"
        )
        val result = injector.injectLiveContext(state, emptyList())
        assertTrue(result.contains("[LIVE_CONTEXT]"))
        assertTrue(result.contains("Intention: test"))
        assertTrue(result.contains("Iteration: 1/10"))
        assertTrue(result.contains("Status: OK"))
        assertFalse(result.contains("[STATIC_CONTEXT]"))
    }

    @Test
    fun `injectLiveContext includes session metadata header`() {
        val state = VibecodingState(
            intention = "Add dark mode toggle",
            workspaceRoot = "/home/user/project",
            iteration = 2,
            maxActions = 10,
            focusLevel = "MODULE"
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("[LIVE_CONTEXT]"))
        assertTrue(result.contains("Intention: Add dark mode toggle"))
        assertTrue(result.contains("Iteration: 3/10"))
        assertTrue(result.contains("Focus: MODULE"))
    }

    @Test
    fun `injectLiveContext includes error when present`() {
        val state = VibecodingState(
            intention = "Fix compilation",
            workspaceRoot = "/tmp",
            error = "Compilation failed: syntax error",
            retryCount = 1,
            maxRetries = 3
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Error: Compilation failed: syntax error"))
        assertTrue(result.contains("Retry: 1/3"))
    }

    @Test
    fun `injectLiveContext includes tool call history from audit entries`() {
        val state = VibecodingState(
            intention = "Build project",
            workspaceRoot = "/tmp",
            iteration = 2,
            executedTasks = listOf("compileKotlin", "runTests")
        )
        val auditEntries = listOf(
            AuditEntry(
                timestamp = Instant.now(),
                tool = "exec_gradle",
                arguments = mapOf("task" to "compileKotlin"),
                dryRun = false,
                result = "BUILD SUCCESSFUL",
                workspaceRoot = "/tmp"
            ),
            AuditEntry(
                timestamp = Instant.now(),
                tool = "exec_gradle",
                arguments = mapOf("task" to "test"),
                dryRun = false,
                result = "3 tests failed",
                error = "Test failures in FooTest",
                workspaceRoot = "/tmp"
            )
        )
        val result = injector.injectLiveContext(state, auditEntries)

        assertTrue(result.contains("Tool call history"))
        assertTrue(result.contains("exec_gradle(compileKotlin)"))
        assertTrue(result.contains("BUILD SUCCESSFUL"))
        assertTrue(result.contains("exec_gradle(test)"))
        assertTrue(result.contains("3 tests failed"))
    }

    @Test
    fun `injectLiveContext includes executed tasks summary`() {
        val state = VibecodingState(
            intention = "Multi-step build",
            workspaceRoot = "/tmp",
            iteration = 3,
            executedTasks = listOf("compileKotlin", "runTests", "publishToMavenLocal")
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Executed tasks"))
        assertTrue(result.contains("compileKotlin"))
        assertTrue(result.contains("runTests"))
        assertTrue(result.contains("publishToMavenLocal"))
    }

    @Test
    fun `injectLiveContext includes static context when provided`() {
        val state = VibecodingState(
            intention = "Add feature",
            workspaceRoot = "/tmp"
        )
        val staticContext = AgentContext(
            eagerRules = "Rule 1: No commits without permission",
            ragChunks = listOf("chunk1: Architecture overview", "chunk2: API docs"),
            graphRelations = "codebase → planner → training",
            backlogItems = listOf("EPIC SP-6: LiveContextInjector")
        )
        val result = injector.injectLiveContext(state, emptyList(), staticContext)

        assertTrue(result.contains("[STATIC_CONTEXT]"))
        assertTrue(result.contains("Rule 1: No commits without permission"))
        assertTrue(result.contains("chunk1: Architecture overview"))
        assertTrue(result.contains("chunk2: API docs"))
        assertTrue(result.contains("codebase → planner → training"))
        assertTrue(result.contains("EPIC SP-6: LiveContextInjector"))
    }

    @Test
    fun `injectLiveContext does not include static context section when null`() {
        val state = VibecodingState(
            intention = "Add feature",
            workspaceRoot = "/tmp",
            iteration = 1
        )
        val result = injector.injectLiveContext(state, emptyList(), null)

        assertFalse(result.contains("[STATIC_CONTEXT]"))
        assertTrue(result.contains("[LIVE_CONTEXT]"))
    }

    @Test
    fun `injectLiveContext includes focus level BIG_PICTURE`() {
        val state = VibecodingState(
            intention = "Architecture decision",
            workspaceRoot = "/tmp",
            focusLevel = "BIG_PICTURE"
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Focus: BIG_PICTURE"))
    }

    @Test
    fun `injectLiveContext includes focus level IMPLEMENTATION`() {
        val state = VibecodingState(
            intention = "Fix typo",
            workspaceRoot = "/tmp",
            focusLevel = "IMPLEMENTATION"
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Focus: IMPLEMENTATION"))
    }

    @Test
    fun `injectLiveContext handles dryRun mode`() {
        val state = VibecodingState(
            intention = "Test dry run",
            workspaceRoot = "/tmp",
            dryRun = true,
            iteration = 0
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Dry run: true"))
    }

    @Test
    fun `injectLiveContext handles finished state`() {
        val state = VibecodingState(
            intention = "Done",
            workspaceRoot = "/tmp",
            finished = true,
            iteration = 5
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Status: FINISHED"))
    }

    @Test
    fun `injectLiveContext handles error state`() {
        val state = VibecodingState(
            intention = "Failed",
            workspaceRoot = "/tmp",
            error = "Permanent failure",
            finished = true
        )
        val result = injector.injectLiveContext(state, emptyList())

        assertTrue(result.contains("Status: ERROR"))
        assertTrue(result.contains("Permanent failure"))
    }

    @Test
    fun `injectLiveContext handles empty static context gracefully`() {
        val state = VibecodingState(
            intention = "Test",
            workspaceRoot = "/tmp"
        )
        val emptyStatic = AgentContext()
        val result = injector.injectLiveContext(state, emptyList(), emptyStatic)

        assertTrue(result.contains("[STATIC_CONTEXT]"))
        assertTrue(result.contains("(none)"))
    }

    @Test
    fun `injectLiveContext truncates long tool results`() {
        val state = VibecodingState(
            intention = "Long output",
            workspaceRoot = "/tmp",
            iteration = 1
        )
        val longResult = "x".repeat(2000)
        val auditEntries = listOf(
            AuditEntry(
                timestamp = Instant.now(),
                tool = "exec_gradle",
                arguments = mapOf("task" to "build"),
                dryRun = false,
                result = longResult,
                workspaceRoot = "/tmp"
            )
        )
        val result = injector.injectLiveContext(state, auditEntries)

        assertTrue(result.contains("exec_gradle(build)"))
        val resultLine = result.lines().first { it.contains("exec_gradle(build)") }
        assertTrue(resultLine.length <= 300)
    }
}
