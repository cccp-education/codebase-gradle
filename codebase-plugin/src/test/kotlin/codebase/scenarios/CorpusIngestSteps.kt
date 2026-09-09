package codebase.scenarios

import codebase.rag.CompositeContextBuilder
import codebase.store.CorpusChunksLoader
import codebase.store.DoubtPolicy
import codebase.store.RetrieveResult
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@corpus-ingest` scenarios (EPIC CB-FPA-RAG US-3).
 *
 * Steps are prefixed "corpus" / "docs section" to avoid glue collisions
 * with other feature step classes sharing the `codebase.scenarios`
 * package (pattern S-088 — RagSocleSteps). Pure BDD: no Gradle task, no
 * network — the corpus file lives in-memory, the store is a fake.
 */
class CorpusIngestSteps(private val world: CorpusIngestWorld) {

    private var corpusFile: File? = null

    private fun writeTemp(content: String): File =
        File.createTempFile("corpus-chunks-", ".json").apply {
            writeText(content, Charsets.UTF_8)
            deleteOnExit()
        }

    @Given("a corpus ingest world is initialized")
    fun `corpus ingest world initialized`() {
        world.ensureInitialized()
        assertNotNull(world, "CorpusIngestWorld should be instantiated by PicoContainer")
    }

    @Given("a codex chunks file with {int} chunks, {int} carrying the ILLISIBLE marker")
    fun `corpus chunks file created`(count: Int, doubtful: Int) {
        corpusFile = writeTemp(CorpusFileBuilder.chunksJson(count, doubtful))
    }

    @Given("a codex chunks file with clean content {string} and doubtful content {string}")
    fun `corpus chunks file two chunks`(clean: String, doubtful: String) {
        corpusFile = writeTemp(CorpusFileBuilder.twoChunksJson(clean, doubtful))
    }

    @Given("{int} codex chunks marked for ingestion with {int} doubtful")
    fun `codex chunks marked for ingestion`(count: Int, doubtful: Int) {
        corpusFile = writeTemp(CorpusFileBuilder.chunksJson(count, doubtful))
        world.markedChunks = DoubtPolicy.mark(CorpusChunksLoader.load(corpusFile!!))
    }

    @When("the corpus chunks are loaded")
    fun `corpus chunks loaded`() {
        world.loadedChunks = CorpusChunksLoader.load(corpusFile!!)
    }

    @When("the corpus chunks are marked for ingestion")
    fun `corpus chunks marked`() {
        world.loadedChunks = CorpusChunksLoader.load(corpusFile!!)
        world.markedChunks = DoubtPolicy.mark(world.loadedChunks)
    }

    @When("the corpus ingestion is executed against the fake store")
    fun `corpus ingestion executed`() {
        world.fakeStore = CorpusFakeRagStore().also { it.record(world.markedChunks) }
    }

    @Then("{int} typed chunks are loaded")
    fun `typed chunks loaded`(expected: Int) {
        assertEquals(expected, world.loadedChunks.size)
    }

    @And("every loaded chunk keeps its source document {string}")
    fun `loaded chunks keep source document`(expected: String) {
        assertTrue(world.loadedChunks.all { it.sourceDocument == expected })
    }

    @Then("{int} chunk is doubtful with confidence {double}")
    fun `chunk doubtful with confidence`(expected: Int, confidence: Double) {
        val doubtful = world.markedChunks.filter { it.doubt.doubtful }
        assertEquals(expected, doubtful.size)
        doubtful.forEach { assertEquals(confidence, it.doubt.confidence) }
    }

    @And("{int} chunk is confident with full confidence")
    fun `chunk confident full confidence`(expected: Int) {
        val confident = world.markedChunks.filterNot { it.doubt.doubtful }
        assertEquals(expected, confident.size)
        confident.forEach { assertEquals(codebase.store.DoubtMetadata.MAX_CONFIDENCE, it.doubt.confidence) }
    }

    @Then("the store receives {int} document(s)")
    fun `store receives documents`(expected: Int) {
        assertEquals(expected, world.fakeStore!!.documentCount)
    }

    @And("the store records {int} chunks with {int} doubtful")
    fun `store records chunks doubtful`(expectedChunks: Int, expectedDoubtful: Int) {
        assertEquals(expectedChunks, world.fakeStore!!.chunkCount)
        assertEquals(expectedDoubtful, world.fakeStore!!.doubtfulCount)
    }

    @Given("a rag store stub returning a doubtful result and a confident result for query {string}")
    fun `rag store stub doubt mix`(query: String) {
        val results = listOf(
            RetrieveResult(
                chunkId = 1L, chunkIndex = 0, chunkText = "shaky OCR page",
                sectionPath = "Chapitre 1", headingLevel = 1,
                sourceDocument = "book", similarity = 0.8, confidence = 0.2, doubtful = true
            ),
            RetrieveResult(
                chunkId = 2L, chunkIndex = 1, chunkText = "referentiel de competences definition",
                sectionPath = "Chapitre 2", headingLevel = 1,
                sourceDocument = "book", similarity = 0.9
            )
        )
        world.fakeStore = CorpusFakeRagStore()
        world.docsSectionSource = results
    }

    @When("the docs context is loaded for the corpus query {string}")
    fun `docs context corpus query`(query: String) {
        val stub = CorpusStubRagStore(results = world.docsSectionSource!!)
        val builder = CompositeContextBuilder(
            workspaceRoot = File(System.getProperty("java.io.tmpdir")),
            vectorStore = codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy"),
            embeddingPipeline = codebase.rag.EmbeddingPipeline(
                codebase.rag.VectorStore("jdbc:postgresql://localhost:5432/dummy", "dummy", "dummy")
            ),
            config = contracts.context.CompositeContextConfig(),
            codexStore = stub,
        )
        val composite = builder.build(query)
        world.docsSection = composite.docsSection
    }

    @Then("the docs section annotates doubtful chunks")
    fun `docs section annotates doubtful`() {
        val docs = world.docsSection!!
        assertTrue(
            docs.contains(codebase.store.DoubtExposure.DOUBT_MARKER),
            "Expected doubtful annotation in docs section, got: $docs"
        )
    }

    @And("the docs section keeps confident chunks unannotated")
    fun `docs section keeps confident unannotated`() {
        val docs = world.docsSection!!
        val confidentLine = docs.lineSequence().firstOrNull {
            it.contains("referentiel de competences") && !it.contains(codebase.store.DoubtExposure.DOUBT_MARKER)
        }
        assertTrue(
            confidentLine != null,
            "Expected a confident (unannotated) chunk line in docs section, got: $docs"
        )
    }
}