package codebase.koog.agentic

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AgenticOntologizerTest {

    private val ontologizer = AgenticOntologizer()

    @Test
    fun `should map PRINCIPES section from fondateur keywords`() {
        val chunk = buildChunk("== Principes Fondateurs\n. Le verbe dit pourquoi, le complement dit quoi.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.PRINCIPES, result.first().taxonomySection)
    }

    @Test
    fun `should map TAXONOMIE section from quatre verbes keywords`() {
        val chunk = buildChunk("== Taxonomie des Taches\nQuatre Verbes : GENERER, COLLECTER, TRANSFORMER, DEPLOYER.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.TAXONOMIE, result.first().taxonomySection)
    }

    @Test
    fun `should map FORMAT_PIVOT section from metadata json keywords`() {
        val chunk = buildChunk("== Format Pivot\nContrat d'interface : metadata.json obligatoire.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.FORMAT_PIVOT, result.first().taxonomySection)
    }

    @Test
    fun `should map CONVENTION_OVER_CONFIGURATION from inference keywords`() {
        val chunk = buildChunk("== Convention Over Configuration\nInference depuis l'arborescence. Magic value -987654321.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.CONVENTION_OVER_CONFIGURATION, result.first().taxonomySection)
    }

    @Test
    fun `should map CONFIG_DOMAINE from namespace extension keywords`() {
        val chunk = buildChunk("== Configuration par Domaine\nChaque borough expose son extension sous un namespace dedie.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.CONFIG_DOMAINE, result.first().taxonomySection)
    }

    @Test
    fun `should map MAPPING section from borough mapping keywords`() {
        val chunk = buildChunk("== Mapping\nBoroughs Existants : Manhattan plannerGenerateSPG → generateSPG.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.MAPPING, result.first().taxonomySection)
    }

    @Test
    fun `should map ROADMAP_IMPLEMENTATION from phase K keywords`() {
        val chunk = buildChunk("== Roadmap d'Implementation\nPhase K-1 : Convention de nommage.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.ROADMAP_IMPLEMENTATION, result.first().taxonomySection)
    }

    @Test
    fun `should map DEPENDANCES from epic G parallel keywords`() {
        val chunk = buildChunk("== Dependances\nEPIC G et EPIC K sont parallelisables.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.DEPENDANCES, result.first().taxonomySection)
    }

    @Test
    fun `should map ORDRE_ATTAQUE from phase 0 bootstrap keywords`() {
        val chunk = buildChunk("== Ordre d'Attaque\nPhase 0 — Bootstrap Artisanal.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.ORDRE_ATTAQUE, result.first().taxonomySection)
    }

    @Test
    fun `should map EXEMPLES_STDOUT from gradlew tasks keywords`() {
        val chunk = buildChunk("== Exemples\n./gradlew tasks --group=generate")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.EXEMPLES_STDOUT, result.first().taxonomySection)
    }

    @Test
    fun `should map CONCLUSION from ce qu'on importe keywords`() {
        val chunk = buildChunk("== Conclusion\nCe qu'on importe : les 4 verbes. Ce qu'on n'importe pas : le code Groovy.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.CONCLUSION, result.first().taxonomySection)
    }

    @Test
    fun `should map UNKNOWN for unrecognized content`() {
        val chunk = buildChunk("== Introduction\nCeci est un paragraphe quelconque sans mot-cle ontologique.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(TaxonomySection.UNKNOWN, result.first().taxonomySection)
    }

    @Test
    fun `should compute high confidence for fully annotated chunk`() {
        val chunk = AgenticChunk(
            id = "abc123",
            sourceFile = "TAXONOMIE_WORKSPACE.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.CONCEPT,
            content = "== Principes Fondateurs\n. Le verbe dit pourquoi.",
            verb = TaxonomyVerb.GENERER,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256..."
        )
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(1.0, result.first().ontologyConfidence, 0.01)
    }

    @Test
    fun `should compute low confidence for unknown section with no annotations`() {
        val chunk = AgenticChunk(
            id = "abc123",
            sourceFile = "unknown.adoc",
            sourceLines = "1-1",
            chunkType = ChunkType.CONCEPT,
            content = "Some random text.",
            verb = null,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = "sha256..."
        )
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(0.0, result.first().ontologyConfidence, 0.01)
    }

    @Test
    fun `should compute medium confidence for known section with partial annotations`() {
        val chunk = AgenticChunk(
            id = "abc123",
            sourceFile = "TAXONOMIE_WORKSPACE.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.CONCEPT,
            content = "== Taxonomie des Taches\nQuatre Verbes.",
            verb = TaxonomyVerb.GENERER,
            domain = null,
            dagLevel = null,
            circle = null,
            weight = 0.5,
            checksum = "sha256..."
        )
        val result = ontologizer.ontologize(listOf(chunk))
        assertEquals(0.6, result.first().ontologyConfidence, 0.01)
    }

    @Test
    fun `should find related chunks by shared domain and verb`() {
        val chunk1 = AgenticChunk(
            id = "id1",
            sourceFile = "AGENT.adoc",
            sourceLines = "1-5",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de commit.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha1"
        )
        val chunk2 = AgenticChunk(
            id = "id2",
            sourceFile = "AGENT.adoc",
            sourceLines = "10-15",
            chunkType = ChunkType.RULE,
            content = "NE JAMAIS lancer de tests.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha2"
        )
        val chunk3 = AgenticChunk(
            id = "id3",
            sourceFile = "BACKLOG.adoc",
            sourceLines = "20-25",
            chunkType = ChunkType.CONCEPT,
            content = "Pipeline de generation.",
            verb = TaxonomyVerb.GENERER,
            domain = "planner",
            dagLevel = DagLevel.N2,
            circle = 4,
            weight = 0.5,
            checksum = "sha3"
        )

        val result = ontologizer.ontologize(listOf(chunk1, chunk2, chunk3))

        val r1 = result.first { it.chunk.id == "id1" }
        assertTrue(r1.relatedChunkIds.contains("id2"), "chunk1 should relate to chunk2 (same domain+verb+dag+circle+type)")
        assertFalse(r1.relatedChunkIds.contains("id3"), "chunk1 should NOT relate to chunk3 (different domain+verb+dag)")
    }

    @Test
    fun `should not self-relate`() {
        val chunk = buildChunk("== Principes\n. Le verbe dit pourquoi.")
        val result = ontologizer.ontologize(listOf(chunk))
        assertFalse(result.first().relatedChunkIds.contains(chunk.id), "Should not relate to itself")
    }

    @Test
    fun `should handle empty chunk list`() {
        val result = ontologizer.ontologize(emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should ontologize multiple chunks from TAXONOMIE_WORKSPACE adoc`() {
        val content = """
            = TAXONOMIE_WORKSPACE — Ontologie
            :date: 2026-05-18

            == Principes Fondateurs

            . Le verbe dit pourquoi, le complement dit quoi.
            . AsciiDoc structure + metadata.json = format pivot universel.

            == Taxonomie des Taches — Quatre Verbes

            GENERER, COLLECTER, TRANSFORMER, DEPLOYER.

            == Format Pivot — Contrat d'Interface

            metadata.json obligatoire en sortie de chaque borough producteur.

            == Convention Over Configuration

            Inference depuis l'arborescence. Magic value -987654321.

            == Configuration par Domaine

            Chaque borough expose son extension sous un namespace dedie.

            == Mapping — Boroughs Existants

            Manhattan plannerGenerateSPG → generateSPG.

            == Roadmap d'Implementation

            Phase K-1 : Convention de nommage.

            == Dependances

            EPIC G et EPIC K sont parallelisables.

            == Ordre d'Attaque

            Phase 0 — Bootstrap Artisanal.

            == Exemples — Avant/Apres

            ./gradlew tasks --group=generate

            == Conclusion

            Ce qu'on importe : les 4 verbes. Ce qu'on n'importe pas : le code Groovy.
        """.trimIndent()

        val chunker = AgenticChunker()
        val chunks = chunker.chunk(content, sourceFile = "TAXONOMIE_WORKSPACE.adoc")
        val result = ontologizer.ontologize(chunks)

        assertTrue(result.isNotEmpty(), "Should produce ontologized chunks")

        val sections = result.map { it.taxonomySection }.toSet()
        assertTrue(sections.contains(TaxonomySection.PRINCIPES))
        assertTrue(sections.contains(TaxonomySection.TAXONOMIE))
        assertTrue(sections.contains(TaxonomySection.FORMAT_PIVOT))
        assertTrue(sections.contains(TaxonomySection.CONVENTION_OVER_CONFIGURATION))
        assertTrue(sections.contains(TaxonomySection.CONFIG_DOMAINE))
        assertTrue(sections.contains(TaxonomySection.MAPPING))
        assertTrue(sections.contains(TaxonomySection.ROADMAP_IMPLEMENTATION))
        assertTrue(sections.contains(TaxonomySection.DEPENDANCES))
        assertTrue(sections.contains(TaxonomySection.ORDRE_ATTAQUE))
        assertTrue(sections.contains(TaxonomySection.EXEMPLES_STDOUT))
        assertTrue(sections.contains(TaxonomySection.CONCLUSION))

        for (r in result) {
            assertTrue(r.ontologyConfidence >= 0.0 && r.ontologyConfidence <= 1.0)
            assertNotNull(r.chunk)
        }
    }

    @Test
    fun `should preserve original chunk data in ontologized result`() {
        val chunk = AgenticChunk(
            id = "preserve-test",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de commit.",
            verb = TaxonomyVerb.INTERDIRE,
            domain = "codebase",
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "abc123def456"
        )
        val result = ontologizer.ontologize(listOf(chunk))
        val ontologized = result.first()
        assertEquals(chunk.id, ontologized.chunk.id)
        assertEquals(chunk.content, ontologized.chunk.content)
        assertEquals(chunk.verb, ontologized.chunk.verb)
        assertEquals(chunk.domain, ontologized.chunk.domain)
        assertEquals(chunk.dagLevel, ontologized.chunk.dagLevel)
        assertEquals(chunk.circle, ontologized.chunk.circle)
        assertEquals(chunk.weight, ontologized.chunk.weight, 0.01)
        assertEquals(chunk.checksum, ontologized.chunk.checksum)
    }

    private fun buildChunk(content: String): AgenticChunk {
        val chunker = AgenticChunker()
        val chunks = chunker.chunk(content, sourceFile = "TAXONOMIE_WORKSPACE.adoc")
        return chunks.first()
    }
}
