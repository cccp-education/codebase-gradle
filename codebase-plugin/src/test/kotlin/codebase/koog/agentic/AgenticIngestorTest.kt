package codebase.koog.agentic

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AgenticIngestorTest {

    private val chunker = AgenticChunker()
    private val ontologizer = AgenticOntologizer()
    private val compiler = AgenticCompiler()
    private lateinit var repo: InMemoryAgenticChunkRepository
    private lateinit var ingestor: AgenticIngestor

    @BeforeEach
    fun setup() {
        repo = InMemoryAgenticChunkRepository()
        ingestor = AgenticIngestor(chunker, ontologizer, repo, compiler)
    }

    @Test
    fun `should ingest a new file and produce chunks and artifacts`() = runBlocking {
        val content = """
            = AGENT.adoc — Directives Agent
            :date: 2026-05-19

            == Regles Absolues

            **INTERDICTION FORMELLE** de commit/push/merge sans permission explicite.

            == Methodologie

            . Lire AGENT.adoc
            . Verifier git status
            . Verifier que le build compile
        """.trimIndent()

        val report = ingestor.ingest(listOf("AGENT.adoc" to content))

        assertTrue(report.chunksAdded > 0, "Should add chunks for new file")
        assertEquals(0, report.chunksSkipped, "No chunks should be skipped on first ingest")
        assertEquals(0, report.chunksModified, "No chunks should be modified on first ingest")
        assertTrue(report.artifactsCompiled > 0, "Should compile artifacts from chunks")
        assertEquals(report.chunksAdded, repo.countChunks(), "Repo should have all added chunks")
    }

    @Test
    fun `should skip unchanged file on second ingest`() = runBlocking {
        val content = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit sans permission.
        """.trimIndent()

        val firstReport = ingestor.ingest(listOf("AGENT.adoc" to content))
        val firstCount = repo.countChunks()

        val secondReport = ingestor.ingest(listOf("AGENT.adoc" to content))

        assertEquals(0, secondReport.chunksAdded, "No new chunks should be added for unchanged file")
        assertEquals(firstCount, secondReport.chunksSkipped, "All chunks should be skipped")
        assertEquals(0, secondReport.chunksModified, "No chunks should be modified")
        assertEquals(firstCount, repo.countChunks(), "Repo count should remain unchanged")
    }

    @Test
    fun `should detect modified chunks and re-ingest them`() = runBlocking {
        val originalContent = """
            = AGENT.adoc
            **INTERDICTION** de commit sans permission.
        """.trimIndent()

        val modifiedContent = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit/push/merge sans permission explicite.
        """.trimIndent()

        ingestor.ingest(listOf("AGENT.adoc" to originalContent))
        val originalCount = repo.countChunks()

        val report = ingestor.ingest(listOf("AGENT.adoc" to modifiedContent))

        assertTrue(report.chunksModified > 0, "Should detect modified chunks")
        assertTrue(repo.countChunks() >= originalCount, "Repo should have updated chunks")
    }

    @Test
    fun `should handle empty file list`() = runBlocking {
        val report = ingestor.ingest(emptyList())

        assertEquals(0, report.chunksAdded)
        assertEquals(0, report.chunksSkipped)
        assertEquals(0, report.chunksModified)
        assertEquals(0, report.artifactsCompiled)
    }

    @Test
    fun `should handle file with blank content`() = runBlocking {
        val report = ingestor.ingest(listOf("empty.adoc" to "   \n  \n   "))

        assertEquals(0, report.chunksAdded)
        assertEquals(0, report.chunksSkipped)
        assertEquals(0, report.chunksModified)
        assertEquals(0, report.artifactsCompiled)
    }

    @Test
    fun `should ingest multiple files in one batch`() = runBlocking {
        val agentContent = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit sans permission.
        """.trimIndent()

        val indexContent = """
            = INDEX.adoc
            :session-en-cours: 095
            == EPIC Y — Agentic Literature Compiler
            Y-5 AgenticIngestor en cours.
        """.trimIndent()

        val report = ingestor.ingest(listOf(
            "AGENT.adoc" to agentContent,
            "INDEX.adoc" to indexContent
        ))

        assertTrue(report.chunksAdded >= 2, "Should add chunks from both files")
        assertTrue(report.artifactsCompiled > 0, "Should compile artifacts")
    }

    @Test
    fun `should produce ingestion report with correct totals`() = runBlocking {
        val content = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit sans permission.
            == Methodologie
            . Lire AGENT.adoc
            . Verifier git status
        """.trimIndent()

        val report = ingestor.ingest(listOf("AGENT.adoc" to content))

        val total = report.chunksAdded + report.chunksSkipped + report.chunksModified
        assertTrue(total > 0, "Report should have non-zero total")
        assertTrue(report.chunksAdded >= report.artifactsCompiled,
            "Artifacts compiled should not exceed chunks added")
    }

    @Test
    fun `should report governance sections when ingestor has governance ontologizer`() = runBlocking {
        val ingestorWithGovernance = AgenticIngestor(
            chunker,
            ontologizer,
            repo,
            compiler,
            GovernanceOntologizer()
        )

        val content = """
            = AGENT.adoc

            **INTERDICTION FORMELLE** de commit sans permission.
        """.trimIndent()

        val report = ingestorWithGovernance.ingest(listOf("AGENT.adoc" to content))

        assertTrue(report.sectionsAdded.containsKey(GovernanceSection.RULES_ABSOLUES),
            "Should report RULES_ABSOLUES section added")
        assertTrue((report.sectionsAdded[GovernanceSection.RULES_ABSOLUES] ?: 0) > 0,
            "Should add at least one chunk to RULES_ABSOLUES")
        assertTrue(report.sectionsTotal.containsKey(GovernanceSection.RULES_ABSOLUES),
            "Should report RULES_ABSOLUES total")
    }

    @Test
    fun `should reject invalid chunks and report chunksInvalid`() = runBlocking {
        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                return if (chunk.content.contains("INVALID")) {
                    ValidationResult(valid = false, errors = listOf("content contains INVALID marker"))
                } else {
                    super.validate(chunk)
                }
            }
        }
        val ingestorWithValidation = AgenticIngestor(
            chunker, ontologizer, repo, compiler,
            governanceOntologizer = null,
            chunkValidator = fakeValidator
        )

        val content = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit sans permission.
            == Section INVALID
            This line contains INVALID marker.
        """.trimIndent()

        val report = ingestorWithValidation.ingest(listOf("AGENT.adoc" to content))

        assertTrue(report.chunksInvalid > 0, "Should report invalid chunks")
        assertTrue(report.validationErrors.any { it.message.contains("INVALID marker") },
            "Should report validation error message")
    }

    @Test
    fun `should not insert invalid chunks into repository`() = runBlocking {
        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                return if (chunk.content.contains("INVALID")) {
                    ValidationResult(valid = false, errors = listOf("invalid"))
                } else {
                    super.validate(chunk)
                }
            }
        }
        val ingestorWithValidation = AgenticIngestor(
            chunker, ontologizer, repo, compiler,
            governanceOntologizer = null,
            chunkValidator = fakeValidator
        )

        val content = """
            = AGENT.adoc
            == Valid rule
            **INTERDICTION FORMELLE** de commit sans permission.
            == Invalid section
            This line contains INVALID marker.
        """.trimIndent()

        val beforeCount = repo.countChunks()
        val report = ingestorWithValidation.ingest(listOf("AGENT.adoc" to content))

        assertTrue(report.chunksInvalid > 0, "Should detect invalid chunks")
        assertTrue(report.chunksAdded > 0, "Should still add valid chunks")
        assertEquals(beforeCount + report.chunksAdded, repo.countChunks(),
            "Repo should only contain valid chunks")
        assertTrue(repo.listChunks(Int.MAX_VALUE).none { it.chunk.content.contains("INVALID marker") },
            "Repository should not contain invalid chunk content")
    }

    @Test
    fun `should report zero invalid chunks when all chunks are valid`() = runBlocking {
        val content = """
            = AGENT.adoc
            **INTERDICTION FORMELLE** de commit sans permission.
        """.trimIndent()

        val report = ingestor.ingest(listOf("AGENT.adoc" to content))

        assertEquals(0, report.chunksInvalid, "All chunks should be valid")
        assertTrue(report.validationErrors.isEmpty(), "Validation errors should be empty")
    }
}
