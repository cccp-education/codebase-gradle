package codebase.koog.plannerport

import codebase.koog.planning.PlanState
import contracts.agent.Epic
import contracts.agent.Plan
import contracts.agent.UserStory
import contracts.context.CompositeContext
import contracts.context.CompositeContextConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlannerPortTest {

    private fun sampleContext(): CompositeContext = CompositeContext(
        eagerSection = "rules",
        ragSection = "docs",
        graphifySection = "relations",
        docsSection = "manuals",
        config = CompositeContextConfig()
    )

    private fun samplePlan(): Plan = Plan(
        title = "Test plan",
        epics = listOf(
            Epic(
                name = "E1", description = "epic", points = 1,
                userStories = listOf(UserStory(description = "US1", tasks = emptyList()))
            )
        ),
        totalPoints = 1,
        estimatedSessions = "1"
    )

    @Test
    fun `port is a fun interface with a single plan method`() {
        val port = PlannerPort { intention, context ->
            PlanState(intention = intention, compositeContext = context, plan = samplePlan())
        }
        val state = port.plan("fix bug", sampleContext())
        assertEquals("fix bug", state.intention)
        assertNotNull(state.plan)
        assertNull(state.error)
    }

    @Test
    fun `default noOpPlanner returns a failed PlanState with planner unavailable error`() {
        val state = PlannerPort.NoOp.plan("fix bug", sampleContext())
        assertEquals("fix bug", state.intention)
        assertEquals(sampleContext(), state.compositeContext)
        assertNull(state.plan, "NoOp fallback must not produce a plan")
        assertTrue(
            state.error?.contains("Planner unavailable", ignoreCase = true) == true,
            "NoOp error should mention planner unavailable but was: ${state.error}"
        )
    }

    @Test
    fun `port preserves the full composite context in the returned state`() {
        val captured = mutableListOf<CompositeContext>()
        val port = PlannerPort { _, context ->
            captured.add(context)
            PlanState(intention = "x", compositeContext = context)
        }
        port.plan("intention", sampleContext())
        assertEquals(1, captured.size)
        assertEquals("rules", captured[0].eagerSection)
        assertEquals("docs", captured[0].ragSection)
        assertEquals("relations", captured[0].graphifySection)
        assertEquals("manuals", captured[0].docsSection)
    }

    @Test
    fun `adapter exceptions propagate to caller — graph owns the fallback`() {
        val port = PlannerPort { _, _ -> throw IllegalStateException("LLM down") }
        var thrown: Exception? = null
        try {
            port.plan("fix bug", sampleContext())
        } catch (e: IllegalStateException) {
            thrown = e
        }
        assertEquals("LLM down", thrown?.message)
    }
}