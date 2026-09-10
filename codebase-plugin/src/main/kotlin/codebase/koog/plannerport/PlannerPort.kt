package codebase.koog.plannerport

import codebase.koog.planning.PlanState
import contracts.context.CompositeContext

/**
 * N1 port for intention decomposition — the inversion point of the
 * historical codebase → planner N2 dependency (EPIC SVO-1, D3/D4).
 *
 * codebase (N1) owns the vibecoding loop and needs a plan; the planner
 * borough (N2) provides the decomposition. Instead of importing
 * `planning.IntentionPlanner` (which created the N1→N2 cycle, killed by
 * the OCR/RAG precedent — pattern S-096/S-199), codebase declares this
 * port and the consumer wires an adapter at Gradle level
 * (`registerPlannerPort`, mirror of `registerLlmBuildService` —
 * PLN-LLM-HUB). Zero `planning.*` import in codebase production code.
 *
 * Fallback: [NoOp] returns a failed [PlanState] ("Planner unavailable")
 * so the pipeline degrades cleanly (resilient mode, no crash) when no
 * adapter is registered — testable without LLM.
 */
fun interface PlannerPort {

    /**
     * Decomposes an [intention] into a [Plan] using the augmented
     * [context] (EAGER + RAG + Graphify + Docs channels).
     *
     * Implementations are expected to catch their own failures and
     * return a failed [PlanState] (pattern `PlannerIntegration.plan`);
     * unexpected exceptions propagate — the calling graph owns the
     * degraded fallback.
     */
    fun plan(intention: String, context: CompositeContext): PlanState

    companion object {
        /**
         * Default port when no planner adapter is wired: returns a
         * failed [PlanState] — degraded, never crashes, never produces
         * a plan.
         */
        val NoOp: PlannerPort = PlannerPort { intention, context ->
            PlanState(
                intention = intention,
                compositeContext = context,
                classification = "simple",
                planJson = "",
                plan = null,
                error = "Planner unavailable — no adapter registered on PlannerPort"
            )
        }
    }
}