package codebase.koog.planning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VibecodingPlanTest {

    @Test
    fun `empty plan should have no steps`() {
        val plan = VibecodingPlan(emptyList())
        assertEquals(0, plan.steps.size)
        assertEquals(RollbackStrategy.STOP_ON_ERROR, plan.rollbackStrategy)
    }

    @Test
    fun `plan with steps should preserve order`() {
        val steps = listOf(
            VibecodingStep("compile", "build", "BUILD SUCCESSFUL"),
            VibecodingStep("test", "test", "All tests passed"),
            VibecodingStep("publish", "publish", "BUILD SUCCESSFUL")
        )
        val plan = VibecodingPlan(steps)
        assertEquals(3, plan.steps.size)
        assertEquals("compile", plan.steps[0].description)
        assertEquals("test", plan.steps[1].description)
        assertEquals("publish", plan.steps[2].description)
    }

    @Test
    fun `step should have default maxRetries of 3`() {
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL")
        assertEquals(3, step.maxRetries)
    }

    @Test
    fun `step should accept custom maxRetries`() {
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL", maxRetries = 5)
        assertEquals(5, step.maxRetries)
    }

    @Test
    fun `step should have null verifyHook by default`() {
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL")
        assertEquals(null, step.verifyHook)
    }

    @Test
    fun `step should accept verifyHook`() {
        val step = VibecodingStep("test", "test", "All tests passed", verifyHook = "grep FAILED")
        assertEquals("grep FAILED", step.verifyHook)
    }

    @Test
    fun `plan should default to STOP_ON_ERROR strategy`() {
        val plan = VibecodingPlan(listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")))
        assertEquals(RollbackStrategy.STOP_ON_ERROR, plan.rollbackStrategy)
    }

    @Test
    fun `plan should accept REVERT_AND_CONTINUE strategy`() {
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.REVERT_AND_CONTINUE
        )
        assertEquals(RollbackStrategy.REVERT_AND_CONTINUE, plan.rollbackStrategy)
    }

    @Test
    fun `plan should accept MARK_SKIPPED strategy`() {
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.MARK_SKIPPED
        )
        assertEquals(RollbackStrategy.MARK_SKIPPED, plan.rollbackStrategy)
    }

    @Test
    fun `plan should accept FALLBACK_HUMAN strategy`() {
        val plan = VibecodingPlan(
            listOf(VibecodingStep("compile", "build", "BUILD SUCCESSFUL")),
            rollbackStrategy = RollbackStrategy.FALLBACK_HUMAN
        )
        assertEquals(RollbackStrategy.FALLBACK_HUMAN, plan.rollbackStrategy)
    }

    @Test
    fun `all four RollbackStrategy values should be distinct`() {
        val values = RollbackStrategy.entries.toSet()
        assertEquals(4, values.size)
        assertTrue(values.contains(RollbackStrategy.STOP_ON_ERROR))
        assertTrue(values.contains(RollbackStrategy.REVERT_AND_CONTINUE))
        assertTrue(values.contains(RollbackStrategy.MARK_SKIPPED))
        assertTrue(values.contains(RollbackStrategy.FALLBACK_HUMAN))
    }

    @Test
    fun `step gradleTask should be accessible`() {
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL")
        assertEquals("build", step.gradleTask)
    }

    @Test
    fun `step expectedOutput should be accessible`() {
        val step = VibecodingStep("compile", "build", "BUILD SUCCESSFUL")
        assertEquals("BUILD SUCCESSFUL", step.expectedOutput)
    }

    @Test
    fun `plan with multiple steps and custom strategy should be valid`() {
        val steps = listOf(
            VibecodingStep("collect corpus", "collectFromCorpus", "Corpus collected", maxRetries = 2),
            VibecodingStep("generate SPG", "generateSPG", "SPG generated", verifyHook = "grep ERROR"),
            VibecodingStep("verify output", "verifyOutput", "Output valid", maxRetries = 1)
        )
        val plan = VibecodingPlan(steps, rollbackStrategy = RollbackStrategy.REVERT_AND_CONTINUE)
        assertEquals(3, plan.steps.size)
        assertEquals(RollbackStrategy.REVERT_AND_CONTINUE, plan.rollbackStrategy)
        assertEquals(2, plan.steps[0].maxRetries)
        assertEquals("grep ERROR", plan.steps[1].verifyHook)
        assertEquals(1, plan.steps[2].maxRetries)
    }
}
