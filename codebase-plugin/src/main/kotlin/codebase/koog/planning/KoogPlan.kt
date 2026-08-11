package codebase.koog.planning

import contracts.agent.Plan
import contracts.context.CompositeContext

/**
 * État du graphe de planification.
 *
 * The plan itself is the N0 shared kernel type [contracts.agent.Plan]
 * (agent-contracts). codebase no longer re-declares a local Plan — the
 * split-brain was eliminated by PLN-CONTRACTS-4 (fusion).
 */
data class PlanState(
    val intention: String = "",
    val compositeContext: CompositeContext? = null,
    val classification: String = "",
    val planJson: String = "",
    val plan: Plan? = null,
    val error: String? = null
)
