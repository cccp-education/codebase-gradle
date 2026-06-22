package codebase.scenarios

import codebase.koog.agentic.AgenticChunk
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.DagLevel
import codebase.koog.agentic.GovernanceOntologizer
import codebase.koog.agentic.GovernanceSection
import codebase.koog.agentic.TaxonomyVerb

class GovernanceOntologizerWorld {

    val ontologizer = GovernanceOntologizer()

    var lastChunk: AgenticChunk? = null
    var lastSection: GovernanceSection? = null

    fun buildChunk(sourceFile: String): AgenticChunk {
        return AgenticChunk(
            id = "id-$sourceFile",
            sourceFile = sourceFile,
            sourceLines = "1-3",
            chunkType = ChunkType.CONCEPT,
            content = "= $sourceFile\n\nSome content.",
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = "checksum-$sourceFile"
        )
    }

    fun reset() {
        lastChunk = null
        lastSection = null
    }
}
