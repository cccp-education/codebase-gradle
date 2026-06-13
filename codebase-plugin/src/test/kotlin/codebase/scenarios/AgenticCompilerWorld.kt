package codebase.scenarios

import codebase.koog.agentic.AgenticCompiler
import codebase.koog.agentic.CompiledArtifact

class AgenticCompilerWorld {

    val compiler = AgenticCompiler()

    var lastCompiledArtifact: CompiledArtifact? = null
    var compiledArtifacts: MutableList<CompiledArtifact> = mutableListOf()
    var compilationCount: Int = 0

    fun reset() {
        lastCompiledArtifact = null
        compiledArtifacts.clear()
        compilationCount = 0
    }
}
