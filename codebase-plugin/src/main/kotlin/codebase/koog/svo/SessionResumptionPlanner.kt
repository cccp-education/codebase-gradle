package codebase.koog.svo

/**
 * Immutable input of the resumption planning (EPIC SVO D7): what a borough
 * still has to do, what it already did, and which intention anchored the
 * last vibecoding session.
 */
data class SessionResumptionState(
    val borough: String,
    val openItems: List<String>,
    val doneItems: List<String>,
    val lastIntention: String,
) {
    init {
        require(borough.isNotBlank()) { "borough must not be blank" }
    }
}

/**
 * Deterministic resumption-prompt generator (EPIC SVO D7): replaces the
 * hand-written PROMPT_REPRISE with a generated one, built from the backlog
 * state + the last session intention. No LLM involved — the LLM only runs
 * decomposition (PlannerPort) and execution (VibecodingGraph).
 *
 * Contract:
 * - open items are listed in backlog order (first = next objective);
 * - the last intention anchors the context;
 * - the done count gives progress visibility;
 * - an empty backlog flips the prompt to a BRAINSTORMING session
 *   (chaînage SVO-5, cadrage global augmenté).
 */
object SessionResumptionPlanner {

    fun plan(state: SessionResumptionState): String = buildString {
        appendLine("= Resumption prompt — ${state.borough}")
        appendLine()
        if (state.openItems.isEmpty()) {
            appendLine("The backlog is complete. BRAINSTORM session expected:")
            appendLine("start a new augmented-context brainstorming on the borough vision")
            appendLine("(autofocus BIG_PICTURE, cadrage global).")
        } else {
            appendLine("Objective of the next session — work the backlog, first item first:")
            appendLine()
            state.openItems.forEachIndexed { index, item ->
                appendLine("  ${index + 1}. $item")
            }
        }
        appendLine()
        appendLine("Last session intention: ${state.lastIntention}")
        appendLine("Items completed so far: ${state.doneItems.size}")
    }
}