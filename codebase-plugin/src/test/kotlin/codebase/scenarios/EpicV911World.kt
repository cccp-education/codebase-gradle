package codebase.scenarios

import codebase.koog.agentic.AgenticChunk
import codebase.koog.agentic.AgenticCompiler
import codebase.koog.agentic.ArtifactPayload
import codebase.koog.agentic.ArtifactType
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.DagLevel
import codebase.koog.agentic.ExecutableArtifact
import codebase.koog.agentic.ExecutionResult
import codebase.koog.agentic.OntologizedChunk
import codebase.koog.agentic.TaxonomySection
import codebase.koog.agentic.TaxonomyVerb

class EpicV911World {

    val compiler = AgenticCompiler()

    var lastChunk: OntologizedChunk? = null
    var lastExecutable: ExecutableArtifact? = null
    var lastExecutionResult: ExecutionResult? = null

    fun buildChunk(
        chunkType: ChunkType,
        verb: TaxonomyVerb?,
        content: String,
        domain: String = "codebase"
    ): OntologizedChunk {
        val agenticChunk = AgenticChunk(
            id = "epic-v-9-11-${chunkType.name}-${verb?.name ?: "none"}-${System.currentTimeMillis()}",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = chunkType,
            content = content.trimIndent().trim(),
            verb = verb,
            domain = domain,
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-v9-11"
        )
        return OntologizedChunk(
            chunk = agenticChunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
    }

    fun reset() {
        lastChunk = null
        lastExecutable = null
        lastExecutionResult = null
    }
}
