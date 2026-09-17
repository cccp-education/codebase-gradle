package codebase.koog.svo

import codebase.koog.plannerport.PlannerPort
import codebase.koog.planning.PlanState
import contracts.context.CompositeContext
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * SVO-4 wiring regression — the `autonomousSession` task must *consume* the
 * [PlannerPort] wired by the planner plugin (SVO-3 `plannerPortService`), not
 * silently fall back to [PlannerPort.NoOp].
 *
 * Found by the Newark A2 dogfooding audit (S-264): `buildVibecodingLoop` built
 * `KoogAugmentedContextGraph()` with the default NoOp port while
 * `PlannerPortResolver` (tested in isolation) was never called — the whole
 * `--borough`/planner path was dead. These tests pin the resolution seam.
 */
class AutonomousSessionPlannerPortWiringTest {

    private class RecordingPort : PlannerPort {
        override fun plan(intention: String, context: CompositeContext): PlanState =
            PlanState(intention = intention)
    }

    /** Faithful mirror of planner's `PlannerPortBuildService` (port via registry). */
    object TestPortRegistry {
        @Volatile
        var port: PlannerPort? = null
    }

    abstract class TestPortService : BuildService<TestPortService.Params> {
        interface Params : BuildServiceParameters
        fun port(): PlannerPort = TestPortRegistry.port ?: PlannerPort.NoOp
    }

    @Test
    fun `task resolves the wired PlannerPort from the canonical shared service`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val port = RecordingPort()
        TestPortRegistry.port = port
        project.gradle.sharedServices.registerIfAbsent(
            PlannerPortResolver.CANONICAL_SERVICE_NAME,
            TestPortService::class.java,
        ) { spec -> spec.maxParallelUsages.set(1) }

        val task = project.tasks.findByName("autonomousSession") as AutonomousSessionTask

        val resolved = task.resolvePlannerPort()

        TestPortRegistry.port = null
        assertSame(port, resolved, "the wired PlannerPort must win over the default NoOp")
    }

    @Test
    fun `task falls back to NoOp when no planner is wired`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val task = project.tasks.findByName("autonomousSession") as AutonomousSessionTask

        val resolved = task.resolvePlannerPort()

        assertFalse(resolved === TestPortRegistry.port, "no wire → degraded fallback")
        assertTrue(
            resolved.plan(
                "x",
                CompositeContext("", "", "", config = contracts.context.CompositeContextConfig()),
            ).error != null,
            "NoOp must return a failed PlanState",
        )
    }
}
