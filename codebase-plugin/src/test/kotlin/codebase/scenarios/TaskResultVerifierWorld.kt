package codebase.scenarios

import codebase.koog.planning.TaskResult
import codebase.koog.planning.TaskResultVerifier

class TaskResultVerifierWorld {
    val verifier = TaskResultVerifier()
    var lastResult: TaskResult? = null

    fun reset() {
        lastResult = null
    }
}
