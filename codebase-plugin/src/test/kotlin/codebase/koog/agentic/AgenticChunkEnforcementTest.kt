package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgenticChunkEnforcementTest {

    private lateinit var enforcement: AgenticChunkEnforcement

    @BeforeEach
    fun setup() {
        enforcement = AgenticChunkEnforcement()
    }

    @Test
    fun `should block forbidden git push`() {
        val chunk = AgenticChunk(
            id = "rule-001",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de git push sans flag --dry-run.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-001"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-001",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de git push sans flag --dry-run.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val result = enforcement.check("exec_shell", mapOf("command" to "git push origin main"))
        assertFalse(result.allowed, "git push should be blocked")
        assertEquals("rule-001", result.blockedBy)
        assertNotNull(result.reason)
    }

    @Test
    fun `should allow git push with dry-run flag`() {
        val chunk = AgenticChunk(
            id = "rule-002",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de git push sans flag --dry-run.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-002"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-002",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de git push sans flag --dry-run.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val result = enforcement.check("exec_shell", mapOf("command" to "git push --dry-run origin main"))
        assertTrue(result.allowed, "git push --dry-run should be allowed")
        assertNull(result.blockedBy)
    }

    @Test
    fun `should allow unrelated tool calls`() {
        val chunk = AgenticChunk(
            id = "rule-003",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de git push sans flag --dry-run.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-003"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-003",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de git push sans flag --dry-run.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val gradleResult = enforcement.check("exec_gradle", mapOf("task" to "build"))
        assertTrue(gradleResult.allowed, "gradle build should be allowed")

        val shellResult = enforcement.check("exec_shell", mapOf("command" to "ls -la"))
        assertTrue(shellResult.allowed, "ls should be allowed")
    }

    @Test
    fun `should not register non-PRE_HOOK artifacts`() {
        val chunk = AgenticChunk(
            id = "rule-004",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de commit sans permission.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-004"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-004",
            artifactType = ArtifactType.CI_GATE,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de commit sans permission.",
            targetHint = "codebase",
            confidence = 0.8
        )

        val registered = enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))
        assertEquals(0, registered, "CI_GATE artifacts should not be registered as enforcement rules")
        assertEquals(0, enforcement.ruleCount())
    }

    @Test
    fun `should not register non-INTERDIRE chunks`() {
        val chunk = AgenticChunk(
            id = "rule-005",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "OBLIGATOIRE de verifier git status avant de coder.",
            verb = TaxonomyVerb.VALIDER,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-005"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-005",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (VALIDER): OBLIGATOIRE de verifier git status.",
            targetHint = "codebase",
            confidence = 0.8
        )

        val registered = enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))
        assertEquals(0, registered, "Non-INTERDIRE chunks should not be registered")
        assertEquals(0, enforcement.ruleCount())
    }

    @Test
    fun `should clear all rules`() {
        val chunk = AgenticChunk(
            id = "rule-006",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de git push sans flag --dry-run.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-006"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-006",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de git push sans flag --dry-run.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))
        assertTrue(enforcement.ruleCount() > 0)

        enforcement.clear()
        assertEquals(0, enforcement.ruleCount())

        val result = enforcement.check("exec_shell", mapOf("command" to "git push origin main"))
        assertTrue(result.allowed, "After clear, all calls should be allowed")
    }

    @Test
    fun `should block forbidden commit`() {
        val chunk = AgenticChunk(
            id = "rule-007",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de commit sans permission explicite.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-007"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-007",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de commit sans permission.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val result = enforcement.check("exec_shell", mapOf("command" to "git commit -m 'test'"))
        assertFalse(result.allowed, "git commit should be blocked")
        assertEquals("rule-007", result.blockedBy)
    }

    @Test
    fun `should block forbidden merge`() {
        val chunk = AgenticChunk(
            id = "rule-008",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de merge sans permission explicite.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-008"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-008",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de merge sans permission.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val result = enforcement.check("exec_shell", mapOf("command" to "git merge feature-branch"))
        assertFalse(result.allowed, "git merge should be blocked")
        assertEquals("rule-008", result.blockedBy)
    }

    @Test
    fun `should block forbidden gradle task`() {
        val chunk = AgenticChunk(
            id = "rule-009",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de ./gradlew publish sans verification.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-009"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-009",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de publish sans verification.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val result = enforcement.check("exec_gradle", mapOf("task" to "publish"))
        assertFalse(result.allowed, "gradle publish should be blocked")
        assertEquals("rule-009", result.blockedBy)
    }

    @Test
    fun `should allow gradle publish with dry-run flag`() {
        val chunk = AgenticChunk(
            id = "rule-010",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de ./gradlew publish sans flag --dry-run.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-010"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        val artifact = CompiledArtifact(
            sourceChunkId = "rule-010",
            artifactType = ArtifactType.PRE_HOOK,
            description = "Rule (INTERDIRE): INTERDICTION FORMELLE de publish sans --dry-run.",
            targetHint = "codebase",
            confidence = 0.8
        )

        enforcement.registerFromCompiled(listOf(artifact), listOf(ontologized))

        val result = enforcement.check("exec_gradle", mapOf("task" to "publish --dry-run"))
        assertTrue(result.allowed, "gradle publish --dry-run should be allowed")
    }
}
