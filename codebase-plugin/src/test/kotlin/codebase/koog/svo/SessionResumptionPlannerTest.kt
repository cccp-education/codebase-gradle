package codebase.koog.svo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionResumptionPlannerTest {

    private fun state(
        open: List<String> = listOf("SVO-2 objects purs", "SVO-3 adapter planner"),
        done: List<String> = listOf("SVO-1 inversion port"),
        intention: String = "EPIC SVO — Session Vibecoding Orchestrée",
    ): SessionResumptionState = SessionResumptionState(
        borough = "codebase",
        openItems = open,
        doneItems = done,
        lastIntention = intention,
    )

    @Test
    fun `prompt mentions borough and next open item first`() {
        val prompt = SessionResumptionPlanner.plan(state())
        assertTrue(prompt.contains("codebase"), "borough must be present")
        val svo2Pos = prompt.indexOf("SVO-2 objects purs")
        val svo3Pos = prompt.indexOf("SVO-3 adapter planner")
        assertTrue(svo2Pos in 0..svo3Pos, "first open item must come first")
    }

    @Test
    fun `prompt is ordered — open items keep their backlog order`() {
        val prompt = SessionResumptionPlanner.plan(
            state(open = listOf("A first", "B second", "C third"))
        )
        assertTrue(
            prompt.indexOf("A first") < prompt.indexOf("B second") &&
                prompt.indexOf("B second") < prompt.indexOf("C third"),
        )
    }

    @Test
    fun `prompt carries the last session intention as context`() {
        val prompt = SessionResumptionPlanner.plan(state())
        assertTrue(prompt.contains("EPIC SVO"), "last intention must anchor the resumption")
    }

    @Test
    fun `prompt reports completed items count`() {
        val prompt = SessionResumptionPlanner.plan(state(done = listOf("a", "b", "c")))
        assertTrue(
            prompt.contains("3"),
            "done count must appear for progress visibility"
        )
    }

    @Test
    fun `empty backlog produces a brainstorming oriented prompt`() {
        val prompt = SessionResumptionPlanner.plan(state(open = emptyList(), done = emptyList()))
        assertTrue(
            prompt.contains("BRAINSTORM", ignoreCase = true),
            "no backlog left → brainstorming session expected"
        )
    }

    @Test
    fun `prompt is deterministic — same state same prompt`() {
        val s = state()
        assertEquals(SessionResumptionPlanner.plan(s), SessionResumptionPlanner.plan(s))
    }

    @Test
    fun `prompt does not contain stale or done items`() {
        val prompt = SessionResumptionPlanner.plan(
            state(open = listOf("only open item"), done = listOf("finished long ago"))
        )
        assertFalse(prompt.contains("finished long ago"), "done items must not leak into prompt")
        assertTrue(prompt.contains("only open item"))
    }

    @Test
    fun `state invariants reject blank borough`() {
        var thrown: IllegalArgumentException? = null
        try {
            SessionResumptionState(borough = "  ", openItems = emptyList(), doneItems = emptyList(), lastIntention = "x")
        } catch (e: IllegalArgumentException) {
            thrown = e
        }
        assertTrue(thrown != null, "blank borough must be rejected")
    }
}