package codebase.scenarios

import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import reactor.core.publisher.Mono
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgenticIngestorSteps(private val world: AgenticIngestorWorld) {

    @Before("@epic_y_5")
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

    @Given("the agentic schema is initialized for ingestion")
    fun `agentic schema initialized`() = runBlocking {
        world.repository.initSchema()
    }

    @Given("an AGENT.adoc file with rules and procedures")
    fun `agent adoc file with rules and procedures`() {
        world.filesToIngest.add("AGENT.adoc" to """
            = AGENT.adoc — Directives Agent
            :date: 2026-05-19

            == Regles Absolues

            **INTERDICTION FORMELLE** de commit/push/merge sans permission explicite.

            == Methodologie

            . Lire AGENT.adoc
            . Verifier git status
            . Verifier que le build compile
        """.trimIndent())
    }

    @Given("an INDEX.adoc file with metadata")
    fun `index adoc file with metadata`() {
        world.filesToIngest.add("INDEX.adoc" to """
            = INDEX — Codebase Gradle
            :toc:
            :date: 2026-06-11
            :session-en-cours: 095

            _Derniere mise a jour_ : 2026-06-11 (Session 094)
        """.trimIndent())
    }

    @Given("a TAXONOMIE_WORKSPACE.adoc file with all taxonomy sections")
    fun `taxonomy workspace file`() {
        world.filesToIngest.add("TAXONOMIE_WORKSPACE.adoc" to """
            = TAXONOMIE_WORKSPACE — Ontologie
            :date: 2026-05-18

            == Principes Fondateurs

            . Le verbe dit pourquoi, le complement dit quoi.

            == Taxonomie des Taches — Quatre Verbes

            GENERER, COLLECTER, TRANSFORMER, DEPLOYER.

            == Format Pivot — Contrat d'Interface

            metadata.json obligatoire en sortie de chaque borough producteur.

            == Convention Over Configuration

            Inference depuis l'arborescence.

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
        """.trimIndent())
    }

    @When("I ingest the files")
    fun `ingest files`() = runBlocking {
        world.lastReport = world.ingestor.ingest(world.filesToIngest)
    }

    @When("I ingest the same files again")
    fun `ingest same files again`() = runBlocking {
        world.lastReport = world.ingestor.ingest(world.filesToIngest)
    }

    @When("I modify the AGENT.adoc content and re-ingest")
    fun `modify and re-ingest`() = runBlocking {
        world.filesToIngest.clear()
        world.filesToIngest.add("AGENT.adoc" to """
            = AGENT.adoc — Directives Agent
            :date: 2026-06-13

            == Regles Absolues

            **INTERDICTION FORMELLE** de commit/push/merge sans permission explicite.

            == Methodologie

            . Lire AGENT.adoc
            . Verifier git status
            . Verifier que le build compile
            . Verifier les tests passent
        """.trimIndent())
        world.lastReport = world.ingestor.ingest(world.filesToIngest)
    }

    @Then("the ingestion report shows chunks added > {int}")
    fun `chunks added greater than`(min: Int) {
        assertNotNull(world.lastReport)
        assertTrue(world.lastReport!!.chunksAdded > min,
            "Expected chunksAdded > $min, got ${world.lastReport!!.chunksAdded}")
    }

    @Then("the ingestion report shows artifacts compiled > {int}")
    fun `artifacts compiled greater than`(min: Int) {
        assertNotNull(world.lastReport)
        assertTrue(world.lastReport!!.artifactsCompiled > min,
            "Expected artifactsCompiled > $min, got ${world.lastReport!!.artifactsCompiled}")
    }

    @Then("the ingestion report shows chunks skipped = {int}")
    fun `chunks skipped equals`(expected: Int) {
        assertNotNull(world.lastReport)
        assertEquals(expected, world.lastReport!!.chunksSkipped)
    }

    @Then("the ingestion report shows chunks skipped > {int}")
    fun `chunks skipped greater than`(min: Int) {
        assertNotNull(world.lastReport)
        assertTrue(world.lastReport!!.chunksSkipped > min,
            "Expected chunksSkipped > $min, got ${world.lastReport!!.chunksSkipped}")
    }

    @Then("the ingestion report shows chunks added = {int}")
    fun `chunks added equals`(expected: Int) {
        assertNotNull(world.lastReport)
        assertEquals(expected, world.lastReport!!.chunksAdded)
    }

    @Then("the ingestion report shows chunks modified > {int}")
    fun `chunks modified greater than`(min: Int) {
        assertNotNull(world.lastReport)
        assertTrue(world.lastReport!!.chunksModified > min,
            "Expected chunksModified > $min, got ${world.lastReport!!.chunksModified}")
    }

    @Then("the database contains the ingested chunks")
    fun `database contains ingested chunks`() = runBlocking {
        val count = world.repository.countChunks()
        assertTrue(count > 0, "Database should contain ingested chunks, got $count")
    }

    @Then("the database contains chunks from multiple files")
    fun `database contains chunks from multiple files`() = runBlocking {
        val chunks = world.repository.listChunks(Int.MAX_VALUE)
        val sourceFiles = chunks.map { it.chunk.sourceFile }.toSet()
        assertTrue(sourceFiles.size >= 2, "Should have chunks from at least 2 files, got: $sourceFiles")
    }

    @Then("the database contains chunks with taxonomy sections PRINCIPES, TAXONOMIE, FORMAT_PIVOT, CONVENTION_OVER_CONFIGURATION, CONFIG_DOMAINE, MAPPING, ROADMAP_IMPLEMENTATION, DEPENDANCES, ORDRE_ATTAQUE, EXEMPLES_STDOUT, and CONCLUSION")
    fun `all taxonomy sections present`() = runBlocking {
        val chunks = world.repository.listChunks(Int.MAX_VALUE)
        val sections = chunks.map { it.taxonomySection }.toSet()
        val expected = setOf(
            codebase.koog.agentic.TaxonomySection.PRINCIPES,
            codebase.koog.agentic.TaxonomySection.TAXONOMIE,
            codebase.koog.agentic.TaxonomySection.FORMAT_PIVOT,
            codebase.koog.agentic.TaxonomySection.CONVENTION_OVER_CONFIGURATION,
            codebase.koog.agentic.TaxonomySection.CONFIG_DOMAINE,
            codebase.koog.agentic.TaxonomySection.MAPPING,
            codebase.koog.agentic.TaxonomySection.ROADMAP_IMPLEMENTATION,
            codebase.koog.agentic.TaxonomySection.DEPENDANCES,
            codebase.koog.agentic.TaxonomySection.ORDRE_ATTAQUE,
            codebase.koog.agentic.TaxonomySection.EXEMPLES_STDOUT,
            codebase.koog.agentic.TaxonomySection.CONCLUSION
        )
        for (section in expected) {
            assertTrue(sections.contains(section), "Should contain taxonomy section $section")
        }
    }

    @Then("the ingestion report shows files scanned = {int}")
    fun `files scanned equals`(expected: Int) {
        assertNotNull(world.lastReport)
        assertEquals(expected, world.lastReport!!.filesScanned)
    }
}
