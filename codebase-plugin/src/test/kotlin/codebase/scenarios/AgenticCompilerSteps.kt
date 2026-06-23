package codebase.scenarios

import codebase.koog.agentic.AgenticChunk
import codebase.koog.agentic.AgenticChunker
import codebase.koog.agentic.AgenticOntologizer
import codebase.koog.agentic.ArtifactType
import codebase.koog.agentic.ChunkType
import codebase.koog.agentic.DagLevel
import codebase.koog.agentic.OntologizedChunk
import codebase.koog.agentic.TaxonomySection
import codebase.koog.agentic.TaxonomyVerb
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgenticCompilerSteps(private val world: AgenticCompilerWorld) {

    @Before("@epic_y_4")
    fun resetWorld() {
        world.reset()
    }

    @Given("a chunk of type {string} with verb {string} and domain {string}")
    fun `chunk with type verb domain`(chunkType: String, verb: String, domain: String) {
        val agenticChunk = AgenticChunk(
            id = "compiler-test-${chunkType}-${verb}",
            sourceFile = "AGENT.adoc",
            sourceLines = "42-48",
            chunkType = ChunkType.valueOf(chunkType),
            content = "INTERDICTION FORMELLE de commit sans permission.",
            verb = TaxonomyVerb.valueOf(verb),
            domain = domain,
            dagLevel = DagLevel.N1,
            circle = 4,
            weight = 1.0,
            checksum = "sha256-test"
        )
        val ontologized = OntologizedChunk(
            chunk = agenticChunk,
            taxonomySection = TaxonomySection.PRINCIPES,
            ontologyConfidence = 0.9,
            relatedChunkIds = emptyList()
        )
        world.lastCompiledArtifact = world.compiler.compile(ontologized)
    }

    @Given("a TAXONOMIE_WORKSPACE document is chunked and ontologized for compilation")
    fun `taxonomy workspace chunked and ontologized for compilation`() {
        val content = """
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

            == Interdictions

            . NE DOIT JAMAIS publier sans permission explicite.
            . NE DOIT JAMAIS supprimer les archives de session.
        """.trimIndent()

        val chunker = AgenticChunker()
        val chunks = chunker.chunk(content, sourceFile = "TAXONOMIE_WORKSPACE.adoc")
        val ontologizer = AgenticOntologizer()
        val ontologized = ontologizer.ontologize(chunks)

        world.compiledArtifacts.clear()
        for (chunk in ontologized) {
            val artifact = world.compiler.compile(chunk)
            if (artifact != null) {
                world.compiledArtifacts.add(artifact)
            }
        }
    }

    @When("I compile the chunk")
    fun `compile chunk`() {
        assertNotNull(world.lastCompiledArtifact, "Chunk should have been compiled")
    }

    @When("I compile all chunks")
    fun `compile all chunks`() {
        world.compilationCount = world.compiledArtifacts.size
    }

    @Then("the compiled artifact type is {string}")
    fun `artifact type is`(expectedType: String) {
        assertNotNull(world.lastCompiledArtifact)
        assertEquals(ArtifactType.valueOf(expectedType), world.lastCompiledArtifact!!.artifactType)
    }

    @Then("the compiled artifact target hint is {string}")
    fun `artifact target hint is`(expectedHint: String) {
        assertNotNull(world.lastCompiledArtifact)
        assertEquals(expectedHint, world.lastCompiledArtifact!!.targetHint)
    }

    @Then("the compiled artifact confidence is at least {double}")
    fun `artifact confidence at least`(minConfidence: Double) {
        assertNotNull(world.lastCompiledArtifact)
        assertTrue(world.lastCompiledArtifact!!.confidence >= minConfidence,
            "Expected confidence >= $minConfidence, got ${world.lastCompiledArtifact!!.confidence}")
    }

    @Then("the compiled artifact description contains {string}")
    fun `artifact description contains`(keyword: String) {
        assertNotNull(world.lastCompiledArtifact)
        assertTrue(world.lastCompiledArtifact!!.description.contains(keyword, ignoreCase = true),
            "Description should contain '$keyword', got: ${world.lastCompiledArtifact!!.description}")
    }

    @Then("at least {int} artifacts are compiled")
    fun `at least N artifacts compiled`(minCount: Int) {
        assertTrue(world.compilationCount >= minCount,
            "Expected at least $minCount compiled artifacts, got $world.compilationCount")
    }

    @Then("the compiled artifacts include type {string}")
    fun `artifacts include type`(expectedType: String) {
        val types = world.compiledArtifacts.map { it.artifactType }.toSet()
        assertTrue(types.contains(ArtifactType.valueOf(expectedType)),
            "Compiled artifacts should include $expectedType, got: $types")
    }

    @Then("the compiled artifacts include types {string} and {string}")
    fun `artifacts include types`(type1: String, type2: String) {
        val types = world.compiledArtifacts.map { it.artifactType }.toSet()
        assertTrue(types.contains(ArtifactType.valueOf(type1)), "Should include $type1")
        assertTrue(types.contains(ArtifactType.valueOf(type2)), "Should include $type2")
    }
}
