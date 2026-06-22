package codebase.scenarios

import codebase.koog.agentic.AgenticChunk
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.ChunkValidator
import codebase.koog.agentic.DagLevel
import codebase.koog.agentic.TaxonomyVerb
import codebase.koog.agentic.ValidationResult
import java.security.MessageDigest

class ChunkValidatorWorld {

    private val chunker = codebase.koog.agentic.AgenticChunker()
    val validator = ChunkValidator()

    var extractedResults: List<ValidationResult> = emptyList()
        private set
    var lastResult: ValidationResult? = null
        private set

    var content: String? = null
    var sourceFile: String? = null
    var chunk: AgenticChunk? = null

    fun extractAndValidate(sourceFile: String, content: String) {
        val chunks = chunker.chunk(content, sourceFile)
        extractedResults = chunks.map { validator.validate(it) }
    }

    fun validate(chunk: AgenticChunk) {
        lastResult = validator.validate(chunk)
    }

    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    fun buildChunk(
        id: String = sha256("AGENT.adoc:1-3:content"),
        sourceFile: String = "AGENT.adoc",
        sourceLines: String = "1-3",
        chunkType: ChunkType = ChunkType.RULE,
        content: String = "content",
        verb: TaxonomyVerb? = TaxonomyVerb.INTERDIRE,
        domain: String? = "codebase",
        dagLevel: DagLevel? = DagLevel.N1,
        circle: Int? = 4,
        weight: Double = 1.0,
        checksum: String = sha256(content)
    ): AgenticChunk = AgenticChunk(
        id = id,
        sourceFile = sourceFile,
        sourceLines = sourceLines,
        chunkType = chunkType,
        content = content,
        verb = verb,
        domain = domain,
        dagLevel = dagLevel,
        circle = circle,
        weight = weight,
        checksum = checksum
    )

    fun reset() {
        extractedResults = emptyList()
        lastResult = null
        content = null
        sourceFile = null
        chunk = null
    }
}