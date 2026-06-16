package codebase.scenarios.ocr

import codebase.infrastructure.PostgresFixture
import codebase.ocr.OcrIngestTask
import codebase.rag.EmbeddingPipeline
import codebase.rag.VectorStore
import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OcrIngestSteps {

    private val log = LoggerFactory.getLogger(OcrIngestSteps::class.java)
    private var tmpDir: File? = null
    private var store: VectorStore? = null
    private var pipeline: EmbeddingPipeline? = null
    private var lastQueryResults: List<codebase.rag.QueryResult>? = null

    @Before("@epic_ocr_4_ingest")
    fun startPgvector() {
        store = VectorStore(PostgresFixture.jdbcUrl, PostgresFixture.username, PostgresFixture.password)
        store!!.initSchema()
        pipeline = EmbeddingPipeline(store!!)
    }

    @After("@epic_ocr_4_ingest")
    fun cleanup() {
        tmpDir?.deleteRecursively()
    }

    @Given("an OCR output file {string} with content:")
    fun createOcrOutputFile(filename: String, content: String) {
        tmpDir = Files.createTempDirectory("ocr-ingest-cucumber").toFile()
        val ocrDir = File(tmpDir, "ocr")
        ocrDir.mkdirs()
        val file = File(ocrDir, filename)
        file.writeText(content.trimIndent(), Charsets.UTF_8)
    }

    @Given("OCR output files:")
    fun createOcrOutputFiles(table: io.cucumber.datatable.DataTable) {
        tmpDir = Files.createTempDirectory("ocr-ingest-cucumber").toFile()
        val ocrDir = File(tmpDir, "ocr")
        ocrDir.mkdirs()
        for (row in table.asLists()) {
            if (row.size < 2) continue
            val name = row[0]
            val content = row[1]
            File(ocrDir, name).writeText(content, Charsets.UTF_8)
        }
    }

    @When("I ingest OCR output into pgvector")
    fun ingestOcrOutput() {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-ingest-cucumber")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()
    }

    @When("I query pgvector for {string}")
    fun queryPgvector(query: String) {
        val queryVector = pipeline!!.embedQuery(query)
        lastQueryResults = store!!.querySimilar(queryVector, topK = 5)
    }

    @Then("at least 1 document is indexed")
    fun atLeastOneDocumentIndexed() {
        assertTrue(store!!.countDocuments() >= 1, "Should have at least 1 document")
    }

    @Then("at least 1 chunk is indexed")
    fun atLeastOneChunkIndexed() {
        assertTrue(store!!.countChunks() >= 1, "Should have at least 1 chunk")
    }

    @Then("all chunks have embeddings")
    fun allChunksHaveEmbeddings() {
        assertTrue(store!!.allEmbeddingsNonNull(), "All chunks should have embeddings")
    }

    @Then("exactly {int} documents are indexed")
    fun exactlyDocumentsIndexed(count: Int) {
        assertEquals(count, store!!.countDocuments())
    }

    @Then("exactly {int} document is indexed")
    fun exactlyOneDocumentIndexed(count: Int) {
        assertEquals(count, store!!.countDocuments())
    }

    @Then("the RAG query returns at least {int} result")
    fun ragQueryReturnsAtLeastOneResult(minResults: Int) {
        assertNotNull(lastQueryResults, "Query results should not be null")
        assertTrue(lastQueryResults!!.size >= minResults,
            "Should return at least $minResults result(s), got ${lastQueryResults!!.size}")
    }

    @Then("at least {int} result contains {string} or {string}")
    fun atLeastOneResultContains(minResults: Int, text1: String, text2: String) {
        assertNotNull(lastQueryResults, "Query results should not be null")
        val matching = lastQueryResults!!.filter { it.text.contains(text1) || it.text.contains(text2) }
        assertTrue(matching.size >= minResults,
            "Should have at least $minResults result(s) containing '$text1' or '$text2', got ${matching.size}")
    }
}
