package codebase.scenarios

import codebase.koog.agentic.AgenticChunk
import codebase.koog.agentic.AgenticChunker
import codebase.koog.agentic.AgenticOntologizer
import codebase.koog.agentic.ChunkRelation
import codebase.koog.agentic.ChunkRelationType
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.DagLevel
import codebase.koog.agentic.OntologizedChunk
import codebase.koog.agentic.TaxonomySection
import codebase.koog.agentic.TaxonomyVerb
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import io.r2dbc.spi.ConnectionFactory
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgenticSchemaSteps(private val world: AgenticSchemaWorld) {

    @Before("@epic_y_3")
    fun cleanupDatabase() = runBlocking {
        world.repository.initSchema()
        val conn = Mono.from(world.connectionFactory.create()).awaitSingle()
        try {
            Mono.from(conn.createStatement("TRUNCATE TABLE chunk_relations, agentic_chunks CASCADE").execute())
                .flatMap { Mono.from(it.rowsUpdated) }.defaultIfEmpty(0L)
                .awaitSingle()
        } finally {
            Mono.from(conn.close()).subscribe()
        }
        world.reset()
    }

    private fun connectionFactory(): ConnectionFactory = world.connectionFactory

    @Given("the agentic schema is initialized")
    fun `agentic schema initialized`() = runBlocking {
        world.repository.initSchema()
    }

    @Given("a TAXONOMIE_WORKSPACE document is chunked and ontologized")
    fun `taxonomy workspace chunked and ontologized`() = runBlocking {
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
        val ontologizer = AgenticOntologizer()
        val ontologized = ontologizer.ontologize(chunks)

        world.repository.insertChunks(ontologized)
        world.insertedChunkIds.addAll(ontologized.map { it.chunk.id })
    }

    @When("I insert an ontologized chunk with id {string}")
    fun `insert ontologized chunk`(id: String) = runBlocking {
        insertChunkWithDefaults(id)
    }

    @When("I insert an ontologized chunk with id {string} and domain {string}")
    fun `insert chunk with domain`(id: String, domain: String) = runBlocking {
        insertChunkWithDefaults(id, domain = domain)
    }

    @When("I insert an ontologized chunk with id {string} and verb {string}")
    fun `insert chunk with verb`(id: String, verb: String) = runBlocking {
        insertChunkWithDefaults(id, verb = TaxonomyVerb.valueOf(verb))
    }

    @When("I insert an ontologized chunk with id {string} and dagLevel {string}")
    fun `insert chunk with dagLevel`(id: String, dagLevel: String) = runBlocking {
        insertChunkWithDefaults(id, dagLevel = DagLevel.valueOf(dagLevel))
    }

    @When("I insert an ontologized chunk with id {string} and taxonomySection {string}")
    fun `insert chunk with taxonomySection`(id: String, taxonomySection: String) = runBlocking {
        insertChunkWithDefaults(id, taxonomySection = TaxonomySection.valueOf(taxonomySection))
    }

    private suspend fun insertChunkWithDefaults(
        id: String,
        verb: TaxonomyVerb = TaxonomyVerb.INTERDIRE,
        domain: String? = "codebase",
        dagLevel: DagLevel = DagLevel.N1,
        taxonomySection: TaxonomySection = TaxonomySection.PRINCIPES
    ) {
        val chunk = AgenticChunk(
            id = id,
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.RULE,
            content = "INTERDICTION FORMELLE de commit sans permission.",
            verb = verb,
            domain = domain,
            dagLevel = dagLevel,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-${id}"
        )
        val ontologized = OntologizedChunk(
            chunk = chunk,
            taxonomySection = taxonomySection,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        world.repository.insertChunk(ontologized)
        world.lastInsertedChunkId = id
    }

    @When("I retrieve the chunk with id {string}")
    fun `retrieve chunk`(id: String) = runBlocking {
        world.lastRetrievedChunk = world.repository.getChunk(id)
    }

    @When("I list all chunks")
    fun `list all chunks`() = runBlocking {
        world.listedChunks = world.repository.listChunks(limit = 100)
    }

    @When("I list chunks by domain {string}")
    fun `list chunks by domain`(domain: String) = runBlocking {
        world.listedChunks = world.repository.listChunksByDomain(domain)
    }

    @When("I list chunks by verb {string}")
    fun `list chunks by verb`(verb: String) = runBlocking {
        world.listedChunks = world.repository.listChunksByVerb(TaxonomyVerb.valueOf(verb))
    }

    @When("I list chunks by DAG level {string}")
    fun `list chunks by dag level`(level: String) = runBlocking {
        world.listedChunks = world.repository.listChunksByDagLevel(DagLevel.valueOf(level))
    }

    @When("I list chunks by taxonomy section {string}")
    fun `list chunks by taxonomy section`(section: String) = runBlocking {
        world.listedChunks = world.repository.listChunksByTaxonomySection(TaxonomySection.valueOf(section))
    }

    @When("I count chunks")
    fun `count chunks`() = runBlocking {
        world.chunkCount = world.repository.countChunks()
    }

    @When("I insert a relation {string} from {string} to {string} with confidence {double}")
    fun `insert relation`(type: String, sourceId: String, targetId: String, confidence: Double) = runBlocking {
        world.lastRelationId = world.repository.insertRelation(
            sourceId, targetId, ChunkRelationType.valueOf(type), confidence
        )
    }

    @When("I get relations for chunk {string}")
    fun `get relations`(chunkId: String) = runBlocking {
        world.listedRelations = world.repository.getRelations(chunkId)
    }

    @When("I count relations")
    fun `count relations`() = runBlocking {
        world.relationCount = world.repository.countRelations()
    }

    @When("I update the embedding for chunk {string} with a 384-dimensional vector")
    fun `update embedding`(id: String) = runBlocking {
        val vectorStr = "[" + (1..384).joinToString(",") { "0.1" } + "]"
        world.repository.updateEmbedding(id, vectorStr)
    }

    @Then("the chunk is persisted successfully")
    fun `chunk persisted`() {
        assertNotNull(world.lastInsertedChunkId)
    }

    @Then("the retrieved chunk has id {string}")
    fun `retrieved chunk has id`(id: String) {
        assertNotNull(world.lastRetrievedChunk)
        assertEquals(id, world.lastRetrievedChunk!!.chunk.id)
    }

    @Then("the retrieved chunk has verb {string}")
    fun `retrieved chunk has verb`(verb: String) {
        assertNotNull(world.lastRetrievedChunk)
        assertEquals(TaxonomyVerb.valueOf(verb), world.lastRetrievedChunk!!.chunk.verb)
    }

    @Then("the retrieved chunk has domain {string}")
    fun `retrieved chunk has domain`(domain: String) {
        assertNotNull(world.lastRetrievedChunk)
        assertEquals(domain, world.lastRetrievedChunk!!.chunk.domain)
    }

    @Then("the retrieved chunk has DAG level {string}")
    fun `retrieved chunk has dag level`(level: String) {
        assertNotNull(world.lastRetrievedChunk)
        assertEquals(DagLevel.valueOf(level), world.lastRetrievedChunk!!.chunk.dagLevel)
    }

    @Then("the retrieved chunk has taxonomy section {string}")
    fun `retrieved chunk has taxonomy section`(section: String) {
        assertNotNull(world.lastRetrievedChunk)
        assertEquals(TaxonomySection.valueOf(section), world.lastRetrievedChunk!!.taxonomySection)
    }

    @Then("the retrieved chunk has ontology confidence {double}")
    fun `retrieved chunk has confidence`(confidence: Double) {
        assertNotNull(world.lastRetrievedChunk)
        assertEquals(confidence, world.lastRetrievedChunk!!.ontologyConfidence, 0.01)
    }

    @Then("the chunk count is {int}")
    fun `chunk count is`(expected: Int) {
        assertEquals(expected, world.chunkCount)
    }

    @Then("the chunk count is at least {int}")
    fun `chunk count is at least`(min: Int) {
        assertTrue(world.chunkCount >= min, "Expected at least $min chunks, got ${world.chunkCount}")
    }

    @Then("the listed chunks contain at least {int} items")
    fun `listed chunks contain at least`(min: Int) {
        assertTrue(world.listedChunks.size >= min, "Expected at least $min chunks, got ${world.listedChunks.size}")
    }

    @Then("all listed chunks have domain {string}")
    fun `all listed chunks have domain`(domain: String) {
        assertTrue(world.listedChunks.isNotEmpty())
        assertTrue(world.listedChunks.all { it.chunk.domain == domain })
    }

    @Then("all listed chunks have verb {string}")
    fun `all listed chunks have verb`(verb: String) {
        assertTrue(world.listedChunks.isNotEmpty())
        assertTrue(world.listedChunks.all { it.chunk.verb == TaxonomyVerb.valueOf(verb) })
    }

    @Then("all listed chunks have DAG level {string}")
    fun `all listed chunks have dag level`(level: String) {
        assertTrue(world.listedChunks.isNotEmpty())
        assertTrue(world.listedChunks.all { it.chunk.dagLevel == DagLevel.valueOf(level) })
    }

    @Then("all listed chunks have taxonomy section {string}")
    fun `all listed chunks have taxonomy section`(section: String) {
        assertTrue(world.listedChunks.isNotEmpty())
        assertTrue(world.listedChunks.all { it.taxonomySection == TaxonomySection.valueOf(section) })
    }

    @Then("the relation is created with a valid id")
    fun `relation created`() {
        assertTrue(world.lastRelationId > 0, "Relation id should be positive")
    }

    @Then("the relations list contains {int} items")
    fun `relations list contains`(expected: Int) {
        assertEquals(expected, world.listedRelations.size)
    }

    @Then("the relations include type {string}")
    fun `relations include type`(type: String) {
        assertTrue(world.listedRelations.any { it.relationType == ChunkRelationType.valueOf(type) })
    }

    @Then("the relation count is {int}")
    fun `relation count is`(expected: Int) {
        assertEquals(expected, world.relationCount)
    }

    @Then("the embedding is stored for chunk {string}")
    fun `embedding stored`(id: String) = runBlocking {
        val conn = Mono.from(world.connectionFactory.create()).awaitSingle()
        try {
            val hasEmbedding = Mono.from(
                conn.createStatement(
                    "SELECT count(*) FROM agentic_chunks WHERE id = $1 AND embedding IS NOT NULL"
                )
                    .bind("$1", id)
                    .execute()
            ).flatMap { result ->
                Mono.from(result.map { row, _ -> row.get(0, Long::class.java)!! })
            }.awaitSingle()
            assertEquals(1L, hasEmbedding, "Embedding should be non-null for chunk $id")
        } finally {
            Mono.from(conn.close()).subscribe()
        }
    }

    @Then("the listed chunks contain taxonomy sections PRINCIPES, TAXONOMIE, FORMAT_PIVOT, CONVENTION_OVER_CONFIGURATION, CONFIG_DOMAINE, MAPPING, ROADMAP_IMPLEMENTATION, DEPENDANCES, ORDRE_ATTAQUE, EXEMPLES_STDOUT, and CONCLUSION")
    fun `all taxonomy sections present`() {
        val expectedSections = setOf(
            TaxonomySection.PRINCIPES,
            TaxonomySection.TAXONOMIE,
            TaxonomySection.FORMAT_PIVOT,
            TaxonomySection.CONVENTION_OVER_CONFIGURATION,
            TaxonomySection.CONFIG_DOMAINE,
            TaxonomySection.MAPPING,
            TaxonomySection.ROADMAP_IMPLEMENTATION,
            TaxonomySection.DEPENDANCES,
            TaxonomySection.ORDRE_ATTAQUE,
            TaxonomySection.EXEMPLES_STDOUT,
            TaxonomySection.CONCLUSION
        )
        val actualSections = world.listedChunks.map { it.taxonomySection }.toSet()
        for (section in expectedSections) {
            assertTrue(actualSections.contains(section), "Should contain taxonomy section $section")
        }
    }
}
