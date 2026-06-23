package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AgenticCompilerTest {

    private val compiler = AgenticCompiler()

    private fun buildChunk(
        id: String = "test-id",
        chunkType: ChunkType = ChunkType.RULE,
        verb: TaxonomyVerb? = TaxonomyVerb.INTERDIRE,
        domain: String? = "codebase",
        dagLevel: DagLevel? = DagLevel.N1,
        content: String = "INTERDICTION FORMELLE de commit sans permission.",
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
    fun `should compile RULE INTERDIRE into PRE_HOOK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de commit/push/merge sans permission."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.PRE_HOOK, artifact!!.artifactType)
        assertEquals("codebase", artifact.targetHint)
        assertTrue(artifact.confidence >= 0.8)
        assertTrue(artifact.description.contains("INTERDICTION"))
    }

    @Test
    fun `should compile RULE INTERDIRE git into PRE_HOOK with git target`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de commit/push/merge sans permission explicite."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.PRE_HOOK, artifact!!.artifactType)
        assertTrue(artifact.description.contains("commit") || artifact.description.contains("git"))
    }

    @Test
    fun `should compile RULE NE_JAMAIS into PRE_HOOK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "NE JAMAIS lancer de tests sans permission."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.PRE_HOOK, artifact!!.artifactType)
        assertTrue(artifact.description.contains("test"))
    }

    @Test
    fun `should compile PROCEDURE VALIDER into VALIDATION`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = TaxonomyVerb.VALIDER,
            content = ". Verifier git status\n. Verifier que le build compile"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.VALIDATION, artifact!!.artifactType)
        assertTrue(artifact.description.contains("Verifier"))
    }

    @Test
    fun `should compile PROCEDURE GENERER into GRADLE_TASK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = TaxonomyVerb.GENERER,
            content = ". Generer le scenario pedagogique global\n. Produit le SPG"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.GRADLE_TASK, artifact!!.artifactType)
        assertTrue(artifact.description.contains("Generer") || artifact.description.contains("SPG"))
    }

    @Test
    fun `should compile CONSTRAINT VALIDER into CONSTRAINT_CHECK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.CONSTRAINT,
            verb = TaxonomyVerb.VALIDER,
            content = "Maximum 50k tokens EAGER (~3000 lignes)"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.CONSTRAINT_CHECK, artifact!!.artifactType)
        assertTrue(artifact.description.contains("50k") || artifact.description.contains("tokens"))
    }

    @Test
    fun `should compile CONCEPT into METADATA`() {
        val chunk = buildChunk(
            chunkType = ChunkType.CONCEPT,
            verb = TaxonomyVerb.GENERER,
            content = "== Principes Fondateurs\n. Le verbe dit pourquoi, le complement dit quoi."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.METADATA, artifact!!.artifactType)
        assertTrue(artifact.description.contains("Principe") || artifact.description.contains("verbe"))
    }

    @Test
    fun `should compile METADATA into METADATA artifact`() {
        val chunk = buildChunk(
            chunkType = ChunkType.METADATA,
            verb = null,
            content = ":date: 2026-05-18\n:toc:\n:session-en-cours: 093"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.METADATA, artifact!!.artifactType)
    }

    @Test
    fun `should compile RULE with no verb into CI_GATE`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = null,
            content = "OBLIGATOIRE de publier apres modification."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.CI_GATE, artifact!!.artifactType)
    }

    @Test
    fun `should compile PROCEDURE with no verb into VALIDATION`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = null,
            content = ". Lire AGENT.adoc\n. Lire INDEX.adoc"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.VALIDATION, artifact!!.artifactType)
    }

    @Test
    fun `should compile CONSTRAINT with no verb into CONSTRAINT_CHECK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.CONSTRAINT,
            verb = null,
            content = "1 fichier a la fois"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.CONSTRAINT_CHECK, artifact!!.artifactType)
    }

    @Test
    fun `should include domain as targetHint`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            domain = "planner"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals("planner", artifact!!.targetHint)
    }

    @Test
    fun `should compute confidence from chunk weight and ontology confidence`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            confidence = 0.9
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertTrue(artifact!!.confidence >= 0.8)
    }

    @Test
    fun `should compile RULE INTERDIRE secrets into PRE_HOOK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de logguer des tokens API."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.PRE_HOOK, artifact!!.artifactType)
        assertTrue(artifact.description.contains("token") || artifact.description.contains("API"))
    }

    @Test
    fun `should compile PROCEDURE DEPLOYER into GRADLE_TASK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = TaxonomyVerb.DEPLOYER,
            content = ". Publier sur gh-pages\n. Deployer le site statique"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.GRADLE_TASK, artifact!!.artifactType)
    }

    @Test
    fun `should compile PROCEDURE TRANSFORMER into GRADLE_TASK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = TaxonomyVerb.TRANSFORMER,
            content = ". Convertir AsciiDoc en PDF\n. Transformer le document"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.GRADLE_TASK, artifact!!.artifactType)
    }

    @Test
    fun `should compile PROCEDURE COLLECTER into GRADLE_TASK`() {
        val chunk = buildChunk(
            chunkType = ChunkType.PROCEDURE,
            verb = TaxonomyVerb.COLLECTER,
            content = ". Importer les donnees AFNOR/REAC\n. Collecter depuis le corpus"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.GRADLE_TASK, artifact!!.artifactType)
    }

    @Test
    fun `should compile CONCEPT with INTERDIRE into PROMPT_TEMPLATE`() {
        val chunk = buildChunk(
            chunkType = ChunkType.CONCEPT,
            verb = TaxonomyVerb.INTERDIRE,
            content = "== Regles Absolues\nINTERDICTION FORMELLE de commit sans permission."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.PROMPT_TEMPLATE, artifact!!.artifactType)
    }

    @Test
    fun `should compile RULE with unexpected verb into CI_GATE fallback`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.GENERER,
            content = "GENERER un rapport sans contrainte"
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals(ArtifactType.CI_GATE, artifact!!.artifactType)
    }

    @Test
    fun `should include source chunk id in compiled artifact`() {
        val chunk = buildChunk(
            id = "rule-42",
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de publish sans permission."
        )

        val artifact = compiler.compile(chunk)
        assertNotNull(artifact)
        assertEquals("rule-42", artifact!!.sourceChunkId)
    }

    @Test
    fun `should compile RULE INTERDIRE gradle task into PRE_HOOK with exec_gradle tool`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "INTERDICTION FORMELLE de lancer ./gradlew publish sans permission."
        )

        val executable = compiler.compileExecutable(chunk)
        assertTrue(executable.payload is ArtifactPayload.PreHookPayload)
        val payload = executable.payload as ArtifactPayload.PreHookPayload
        assertEquals("exec_gradle", payload.toolName)
        assertTrue(payload.forbiddenPatterns.contains("publish"))
    }

    @Test
    fun `should compile RULE INTERDIRE publish into POST_HOOK when verb mapping changes`() {
        val chunk = buildChunk(
            chunkType = ChunkType.RULE,
            verb = TaxonomyVerb.INTERDIRE,
            content = "NE JAMAIS publier sans validation CI."
        )

        val executable = compiler.compileExecutable(chunk)
        assertEquals(ArtifactType.PRE_HOOK, executable.compiledArtifact.artifactType)
        val payload = executable.payload as ArtifactPayload.PreHookPayload
        assertTrue(payload.forbiddenPatterns.isNotEmpty())
    }
}
