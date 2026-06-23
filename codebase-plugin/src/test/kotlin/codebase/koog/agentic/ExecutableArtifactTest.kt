package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExecutableArtifactTest {

    private val compiler = AgenticCompiler()

    private fun buildChunk(
        id: String = "test-id",
        chunkType: ChunkType = ChunkType.RULE,
        verb: TaxonomyVerb? = TaxonomyVerb.INTERDIRE,
        domain: String? = "codebase",
        dagLevel: DagLevel? = DagLevel.N1,
        content: String = "INTERDICTION FORMELLE de commit/push/merge sans permission.",
        taxonomySection: TaxonomySection = TaxonomySection.PRINCIPES,
        confidence: Double = 0.9
    ): OntologizedChunk {
        val agenticChunk = AgenticChunk(
            id = id,
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = chunkType,
            content = content,
            verb = verb,
            domain = domain,
            dagLevel = dagLevel,
            circle = 4,
            weight = 1.0,
            checksum = "abc123"
        )
        return OntologizedChunk(
            chunk = agenticChunk,
            taxonomySection = taxonomySection,
            ontologyConfidence = confidence,
            relatedChunkIds = emptyList()
        )
    }

    @Test
    fun `compileExecutable should produce PreHookPayload for RULE INTERDIRE`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de git push sans permission."
        )

        val executable = compiler.compileExecutable(chunk)

        assertNotNull(executable)
        assertEquals(ArtifactType.PRE_HOOK, executable!!.compiledArtifact.artifactType)
        assertTrue(executable.payload is ArtifactPayload.PreHookPayload)
        val payload = executable.payload as ArtifactPayload.PreHookPayload
        assertEquals("exec_shell", payload.toolName)
        assertTrue(payload.forbiddenPatterns.contains("push"))
    }

    @Test
    fun `execute should block git push via exec_shell`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de git push sans permission."
        )

        val executable = compiler.compileExecutable(chunk)!!
        val result = executable.execute("exec_shell", mapOf("command" to "git push origin main"))

        assertFalse(result.allowed)
        assertEquals(chunk.chunk.id, result.ruleId)
        assertNotNull(result.reason)
    }

    @Test
    fun `execute should allow git push with dry-run flag`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de git push sauf avec --dry-run."
        )

        val executable = compiler.compileExecutable(chunk)!!
        val result = executable.execute("exec_shell", mapOf("command" to "git push --dry-run"))

        assertTrue(result.allowed)
    }

    @Test
    fun `execute should ignore other tool names for PreHook`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de git push sans permission."
        )

        val executable = compiler.compileExecutable(chunk)!!
        val result = executable.execute("exec_gradle", mapOf("task" to "git push origin main"))

        assertTrue(result.allowed)
    }

    @Test
    fun `compileExecutable should produce GradleTaskPayload for PROCEDURE GENERER`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = TaxonomyVerb.GENERER,
            content = ". Generer le scenario pedagogique global"
        )

        val executable = compiler.compileExecutable(chunk)

        assertNotNull(executable)
        assertTrue(executable!!.payload is ArtifactPayload.GradleTaskPayload)
        val payload = executable.payload as ArtifactPayload.GradleTaskPayload
        assertEquals("generateArtifact", payload.taskName)
    }

    @Test
    fun `compileExecutable should produce ConstraintPayload with bounds`() {
        val chunk = buildChunk(
            chunkType = ChunkType.CONSTRAINT,
            verb = TaxonomyVerb.VALIDER,
            content = "Maximum 50k tokens EAGER (~3000 lignes)"
        )

        val executable = compiler.compileExecutable(chunk)!!

        assertTrue(executable.payload is ArtifactPayload.ConstraintPayload)
        val payload = executable.payload as ArtifactPayload.ConstraintPayload
        assertEquals(50_000, payload.maxTokens)
        assertEquals(3000, payload.maxLines)
    }

    @Test
    fun `compileExecutable should produce MetadataPayload from attribute line`() {
        val chunk = buildChunk(
            chunkType = ChunkType.METADATA,
            verb = null,
            content = ":date: 2026-06-23\n:session: 147"
        )

        val executable = compiler.compileExecutable(chunk)!!

        assertTrue(executable.payload is ArtifactPayload.MetadataPayload)
        val payload = executable.payload as ArtifactPayload.MetadataPayload
        assertEquals("date", payload.metadataKey)
        assertEquals("2026-06-23", payload.metadataValue)
    }

    @Test
    fun `compile should remain backward compatible returning CompiledArtifact`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de git push sans permission."
        )

        val artifact = compiler.compile(chunk)

        assertNotNull(artifact)
        assertEquals(ArtifactType.PRE_HOOK, artifact!!.artifactType)
        assertTrue(artifact.payload is ArtifactPayload.PreHookPayload)
    }
}
