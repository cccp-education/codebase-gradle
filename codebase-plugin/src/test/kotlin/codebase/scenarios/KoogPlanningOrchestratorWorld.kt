package codebase.scenarios

import codebase.koog.state.AugmentedState
import codebase.koog.KoogPlanningOrchestrator
import java.io.File

class KoogPlanningOrchestratorWorld {

    val workspaceRoot: File = File("/tmp/planning-orchestrator-test").also { it.mkdirs() }
    var intention: String = ""
    val orchestrator: KoogPlanningOrchestrator by lazy { KoogPlanningOrchestrator() }
    var resultState: AugmentedState? = null
}
