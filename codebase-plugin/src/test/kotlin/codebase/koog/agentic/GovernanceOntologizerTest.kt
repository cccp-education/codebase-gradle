package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GovernanceOntologizerTest {

    private val ontologizer = GovernanceOntologizer()

    @Test
    fun `should classify AGENT adoc as RULES_ABSOLUES`() {
        val chunk = buildChunk(sourceFile = "AGENT.adoc")
        assertEquals(GovernanceSection.RULES_ABSOLUES, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify INDEX adoc as ETAT_EPICS`() {
        val chunk = buildChunk(sourceFile = ".agents/INDEX.adoc")
        assertEquals(GovernanceSection.ETAT_EPICS, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify BACKLOG adoc as BACKLOG_ITEMS`() {
        val chunk = buildChunk(sourceFile = "BACKLOG.adoc")
        assertEquals(GovernanceSection.BACKLOG_ITEMS, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify SESSIONS_HISTORY adoc as HISTORIQUE`() {
        val chunk = buildChunk(sourceFile = ".agents/SESSIONS_HISTORY.adoc")
        assertEquals(GovernanceSection.HISTORIQUE, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify TEST_COVERAGE_ANALYSIS adoc as COVERAGE`() {
        val chunk = buildChunk(sourceFile = ".agents/TEST_COVERAGE_ANALYSIS.adoc")
        assertEquals(GovernanceSection.COVERAGE, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify PROMPT_REPRISE adoc as MISSION`() {
        val chunk = buildChunk(sourceFile = "PROMPT_REPRISE.adoc")
        assertEquals(GovernanceSection.MISSION, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify unknown adoc as UNKNOWN`() {
        val chunk = buildChunk(sourceFile = "README.adoc")
        assertEquals(GovernanceSection.UNKNOWN, ontologizer.classify(chunk))
    }

    @Test
    fun `should classify subproject AGENT adoc as RULES_ABSOLUES`() {
        val chunk = buildChunk(sourceFile = "my-plugin/AGENT.adoc")
        assertEquals(GovernanceSection.RULES_ABSOLUES, ontologizer.classify(chunk))
    }

    private fun buildChunk(sourceFile: String): AgenticChunk {
        val content = "= $sourceFile\n\nSome content."
        return AgenticChunk(
            id = "id-$sourceFile",
            sourceFile = sourceFile,
            sourceLines = "1-3",
            chunkType = ChunkType.CONCEPT,
            content = content,
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = "checksum-$sourceFile"
        )
    }
}
