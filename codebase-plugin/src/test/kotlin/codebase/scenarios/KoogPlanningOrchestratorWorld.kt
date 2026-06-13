package codebase.scenarios

import vibecoding.contracts.state.AugmentedState
import codebase.koog.KoogPlanningOrchestrator
import java.io.File

class KoogPlanningOrchestratorWorld {

    val workspaceRoot: File = File("/tmp/planning-orchestrator-test").also { it.mkdirs() }
    var intention: String = ""
    val orchestrator: KoogPlanningOrchestrator by lazy { KoogPlanningOrchestrator() }
    var resultState: AugmentedState? = null
}
