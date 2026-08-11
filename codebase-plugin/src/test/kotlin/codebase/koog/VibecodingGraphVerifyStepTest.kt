package codebase.koog

import codebase.koog.planning.VibecodingStep
import codebase.koog.state.VibecodingState
import contracts.agent.Epic
import contracts.agent.GradleTask
import contracts.agent.Plan
import contracts.agent.TaskType
import contracts.agent.UserStory
import contracts.vibecoding.registry.ToolRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * PLN-VERIFY US-3 — codebase cross-borough.
 *
 * Proves that [VibecodingGraph.extractCurrentStep] consumes the N0
 * verification metadata (`expectedOutput` / `maxRetries` / `verifyHook`)
 * declared on [GradleTask] by planner, instead of hardcoding
 * `"BUILD SUCCESSFUL"` / `3` / `null`.
 *
 * Baby-step TDD — RED first: `extractCurrentStep` is private and
 * hardcodes the metadata, so these tests cannot compile/pass yet.
 */
class VibecodingGraphVerifyStepTest {

    private val graph = VibecodingGraph(
        augmentedGraph = null,
        toolRegistry = ToolRegistry()
    )

    @Test
    fun `extractCurrentStep should forward task expectedOutput when custom`() {
        val plan = planWith(
            GradleTask(
                description = "generate SPG",
                gradleTask = "generateSPG",
                expectedOutput = "SPG generated"
            )
        )
        val state = VibecodingState(
            intention = "Generate SPG",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.expectedOutput).isEqualTo("SPG generated")
    }

    @Test
    fun `extractCurrentStep should preserve default expectedOutput when task omits it`() {
        val plan = planWith(
            GradleTask(
                description = "compile",
                gradleTask = "compileKotlin"
            )
        )
        val state = VibecodingState(
            intention = "Compile",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.expectedOutput).isEqualTo("BUILD SUCCESSFUL")
    }

    @Test
    fun `extractCurrentStep should forward task maxRetries when custom`() {
        val plan = planWith(
            GradleTask(
                description = "flaky test",
                gradleTask = "test",
                maxRetries = 5
            )
        )
        val state = VibecodingState(
            intention = "Run tests",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.maxRetries).isEqualTo(5)
    }

    @Test
    fun `extractCurrentStep should preserve default maxRetries when task omits it`() {
        val plan = planWith(
            GradleTask(
                description = "compile",
                gradleTask = "compileKotlin"
            )
        )
        val state = VibecodingState(
            intention = "Compile",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.maxRetries).isEqualTo(3)
    }

    @Test
    fun `extractCurrentStep should forward task verifyHook when present`() {
        val plan = planWith(
            GradleTask(
                description = "audit logs",
                gradleTask = "audit",
                verifyHook = "grep FAILED"
            )
        )
        val state = VibecodingState(
            intention = "Audit",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.verifyHook).isEqualTo("grep FAILED")
    }

    @Test
    fun `extractCurrentStep should preserve default verifyHook null when task omits it`() {
        val plan = planWith(
            GradleTask(
                description = "compile",
                gradleTask = "compileKotlin"
            )
        )
        val state = VibecodingState(
            intention = "Compile",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.verifyHook).isNull()
    }

    @Test
    fun `extractCurrentStep should forward all three metadata together`() {
        val plan = planWith(
            GradleTask(
                description = "publish docs",
                gradleTask = "publishDocs",
                expectedOutput = "Docs published",
                maxRetries = 2,
                verifyHook = "grep '404'"
            )
        )
        val state = VibecodingState(
            intention = "Publish",
            workspaceRoot = "/tmp",
            plan = plan
        )

        val step = graph.extractCurrentStep(state)!!

        assertThat(step.expectedOutput).isEqualTo("Docs published")
        assertThat(step.maxRetries).isEqualTo(2)
        assertThat(step.verifyHook).isEqualTo("grep '404'")
    }

    @Test
    fun `extractCurrentStep should return null when plan is null`() {
        val state = VibecodingState(
            intention = "No plan",
            workspaceRoot = "/tmp",
            plan = null
        )

        val step: VibecodingStep? = graph.extractCurrentStep(state)

        assertThat(step).isNull()
    }

    @Test
    fun `extractCurrentStep should return null when all tasks already executed`() {
        val plan = planWith(
            GradleTask(
                description = "done task",
                gradleTask = "build"
            )
        )
        val state = VibecodingState(
            intention = "Done",
            workspaceRoot = "/tmp",
            plan = plan,
            executedTasks = listOf("done task")
        )

        val step: VibecodingStep? = graph.extractCurrentStep(state)

        assertThat(step).isNull()
    }

    private fun planWith(task: GradleTask): Plan = Plan(
        title = "verify-test",
        epics = listOf(
            Epic(
                name = "E1",
                description = "test",
                points = 1,
                userStories = listOf(
                    UserStory(description = "US1", tasks = listOf(task))
                )
            )
        ),
        totalPoints = 1,
        estimatedSessions = "1"
    )
}