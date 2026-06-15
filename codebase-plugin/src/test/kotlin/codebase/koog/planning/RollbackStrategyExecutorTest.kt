package codebase.koog.planning

import contracts.vibecoding.registry.ToolRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RollbackStrategyExecutorTest {

    private val workspaceRoot = "/tmp/test-workspace"

    @Test
    fun `STOP_ON_ERROR should mark state finished with error`() {
        val registry = ToolRegistry()
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(retryCount = 3, maxRetries = 3)
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.STOP_ON_ERROR
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep)

        assertTrue(result.finished)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("STOP_ON_ERROR"))
    }

    @Test
    fun `REVERT_AND_CONTINUE should git checkout and continue`() {
        val gitCommands = mutableListOf<String>()
        val registry = ToolRegistry()
        registry.registerHandler("exec_shell") { _, args, _ ->
            gitCommands.add(args["command"] ?: "")
            "EXIT 0\nChecked out files"
        }
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(
            retryCount = 3,
            maxRetries = 3,
            executedTasks = listOf("compile")
        )
        val plan = VibecodingPlan(
            listOf(
                VibecodingStep("compile", "build", "BUILD SUCCESSFUL"),
                VibecodingStep("test", "test", "All tests passed")
            ),
            rollbackStrategy = RollbackStrategy.REVERT_AND_CONTINUE
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep, listOf("src/main/Foo.kt"))

        assertNull(result.error)
        assertEquals(0, result.retryCount)
        assertTrue(!result.finished)
        assertTrue(gitCommands.isNotEmpty())
        assertTrue(gitCommands.any { it.contains("git checkout") })
    }

    @Test
    fun `REVERT_AND_CONTINUE without modified files should still continue`() {
        val registry = ToolRegistry()
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(retryCount = 3, maxRetries = 3)
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.REVERT_AND_CONTINUE
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep)

        assertNull(result.error)
        assertEquals(0, result.retryCount)
        assertTrue(!result.finished)
    }

    @Test
    fun `MARK_SKIPPED should mark step skipped and continue`() {
        val registry = ToolRegistry()
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(
            retryCount = 3,
            maxRetries = 3,
            executedTasks = listOf("compile")
        )
        val plan = VibecodingPlan(
            listOf(
                VibecodingStep("compile", "build", "BUILD SUCCESSFUL"),
                VibecodingStep("test", "test", "All tests passed")
            ),
            rollbackStrategy = RollbackStrategy.MARK_SKIPPED
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep)

        assertNull(result.error)
        assertEquals(0, result.retryCount)
        assertTrue(!result.finished)
        assertTrue(result.executedTasks.contains("compile"))
        assertTrue(result.lastToolResult.contains("SKIPPED"))
    }

    @Test
    fun `FALLBACK_HUMAN should pause and request human input`() {
        val registry = ToolRegistry()
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(retryCount = 3, maxRetries = 3)
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.FALLBACK_HUMAN
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep)

        assertNotNull(result.error)
        assertTrue(result.error!!.contains("FALLBACK_HUMAN"))
        assertTrue(result.error!!.contains("compile"))
        assertTrue(result.finished)
    }

    @Test
    fun `STOP_ON_ERROR should include step description in error`() {
        val registry = ToolRegistry()
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(retryCount = 3, maxRetries = 3)
        val plan = VibecodingPlan(
            listOf(VibecodingStep("generate SPG", "generateSPG", "SPG generated")),
            rollbackStrategy = RollbackStrategy.STOP_ON_ERROR
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep)

        assertNotNull(result.error)
        assertTrue(result.error!!.contains("generate SPG"))
    }

    @Test
    fun `MARK_SKIPPED should not modify retryCount`() {
        val registry = ToolRegistry()
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(retryCount = 2, maxRetries = 3)
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.MARK_SKIPPED
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep)

        assertEquals(0, result.retryCount)
    }

    @Test
    fun `REVERT_AND_CONTINUE should reset retryCount`() {
        val gitCommands = mutableListOf<String>()
        val registry = ToolRegistry()
        registry.registerHandler("exec_shell") { _, args, _ ->
            gitCommands.add(args["command"] ?: "")
            "EXIT 0\nChecked out files"
        }
        val executor = RollbackStrategyExecutor(registry, workspaceRoot)
        val state = vibecodingState(retryCount = 5, maxRetries = 3)
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.REVERT_AND_CONTINUE
        )
        val failedStep = plan.steps[0]

        val result = executor.execute(state, plan, failedStep, listOf("src/main/Foo.kt"))

        assertEquals(0, result.retryCount)
    }

    private fun vibecodingState(
        retryCount: Int = 0,
        maxRetries: Int = 3,
        executedTasks: List<String> = emptyList()
    ) = vibecoding.contracts.state.VibecodingState(
        intention = "test",
        workspaceRoot = workspaceRoot,
        retryCount = retryCount,
        maxRetries = maxRetries,
        executedTasks = executedTasks
    )
}
