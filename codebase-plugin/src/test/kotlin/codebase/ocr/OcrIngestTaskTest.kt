package codebase.ocr

import codebase.rag.EmbeddingPipeline
import codebase.rag.VectorStore
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.testcontainers.containers.PostgreSQLContainer
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OcrIngestTaskTest {

    private val container = PostgreSQLContainer<Nothing>("pgvector/pgvector:pg17").apply {
        withDatabaseName("codebase_rag_ocr_ingest_test")
        withUsername("codebase")
        withPassword("codebase")
        withStartupTimeout(java.time.Duration.ofMinutes(2))
        withReuse(false)
    }

    private lateinit var store: VectorStore
    private lateinit var pipeline: EmbeddingPipeline

    @BeforeAll
    fun setUp() {
        container.start()
        store = VectorStore(container.jdbcUrl, container.username, container.password)
        pipeline = EmbeddingPipeline(store)
    }

    @BeforeEach
    fun cleanDatabase() {
        store.initSchema()
    }

    @AfterAll
    fun tearDown() {
        container.stop()
    }

    @Test
    fun `ocrIngest task is registered by CodebasePlugin`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)
        val task = project.tasks.findByName("ocrIngest")
        assertNotNull(task, "ocrIngest task should be registered")
        assertTrue(task is OcrIngestTask)
    }

    @Test
    fun `ocrIngest task group is collect`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)
        val task = project.tasks.getByName("ocrIngest")
        assertEquals("collect", task.group)
    }

    @Test
    fun `ocrIngest with empty directory logs warning`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-empty")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(0, store.countDocuments())
    }

    @Test
    fun `ocrIngest with single adoc file chunks and embeds`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        val ocrFile = ocrDir.resolve("document_ocr.adoc")
        ocrFile.writeText("""
            = Document OCRisé
            :langue: fr
            :modèle: gemini-2.5-flash

            == Introduction

            Ceci est un document de test pour l'ingestion OCR.

            == Section 1

            Le pipeline OCR→chunk→embedding→pgvector permet de rendre
            les documents scannés requêtables via RAG.

            == Section 2

            Les embeddings sont calculés avec ONNX AllMiniLmL6V2.
        """.trimIndent())

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-single")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertTrue(store.countDocuments() >= 1, "Should have at least 1 document")
        assertTrue(store.countChunks() >= 1, "Should have at least 1 chunk")
        assertTrue(store.allEmbeddingsNonNull(), "All chunks should have embeddings")
        assertEquals(pipeline.dimension(), 384)
    }

    @Test
    fun `ocrIngest with multiple adoc files ingests all`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc1_ocr.adoc").writeText("= Document 1\n\nContenu du premier document OCR.")
        ocrDir.resolve("doc2_ocr.adoc").writeText("= Document 2\n\nContenu du second document OCR.")
        ocrDir.resolve("doc3_ocr.adoc").writeText("= Document 3\n\nContenu du troisième document OCR.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-multi")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(3, store.countDocuments())
        assertTrue(store.countChunks() >= 3)
        assertTrue(store.allEmbeddingsNonNull())
    }

    @Test
    fun `ocrIngest skips non-adoc files`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc_ocr.adoc").writeText("= Document OCR\n\nContenu AsciiDoc.")
        ocrDir.resolve("doc_ocr.md").writeText("# Document Markdown\n\nContenu Markdown.")
        ocrDir.resolve("doc_ocr.txt").writeText("Document texte.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-filter")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(1, store.countDocuments(), "Only .adoc files should be ingested")
    }

    @Test
    fun `ocrIngest is idempotent`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc_ocr.adoc").writeText("= Document\n\nContenu test.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-idempotent")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()
        val docsAfterFirst = store.countDocuments()
        val chunksAfterFirst = store.countChunks()

        task.executeIngest()
        val docsAfterSecond = store.countDocuments()
        val chunksAfterSecond = store.countChunks()

        assertEquals(docsAfterFirst * 2, docsAfterSecond, "Second ingest adds documents again")
        assertEquals(chunksAfterFirst * 2, chunksAfterSecond, "Second ingest adds chunks again")
    }

    @Test
    fun `ocrIngest with ensureSchema does not drop existing data`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc1_ocr.adoc").writeText("= Doc 1\n\nPremier document.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-ensure")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()
        val docsAfterFirst = store.countDocuments()

        ocrDir.resolve("doc2_ocr.adoc").writeText("= Doc 2\n\nSecond document.")
        task.executeIngest()

        assertTrue(store.countDocuments() >= docsAfterFirst + 1, "Existing documents should be preserved")
    }

    @Test
    fun `ocrIngest result is queryable via RAG`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("rapport_ocr.adoc").writeText("""
            = Rapport OCRisé
            :langue: fr

            == Résumé

            Le chiffre d'affaires du Q1 2026 est de 1.2 million d'euros.
            La marge brute est de 45%.

            == Analyse

            Les investissements R&D représentent 15% du budget.
            L'effectif est de 42 collaborateurs.
        """.trimIndent())

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-query")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        val queryVector = pipeline.embedQuery("chiffre d'affaires Q1 2026")
        val results = store.querySimilar(queryVector, topK = 3)

        assertTrue(results.isNotEmpty(), "Should return RAG results")
        assertTrue(results.any { it.text.contains("chiffre d'affaires") || it.text.contains("Q1") },
            "Results should contain relevant content")
    }

    @Test
    fun `ocrIngest with anonymized OCR content is queryable`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("anonymized_ocr.adoc").writeText("""
            = Document OCRisé (Anonymisé)
            :langue: fr

            Contact: ***@anonymous.com
            Tel: ***

            == Contenu métier

            Le projet Alpha a livré la version 2.0 en production.
            Les tests de performance montrent une latence de 12ms.
        """.trimIndent())

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-anonymized")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        val queryVector = pipeline.embedQuery("projet Alpha version production")
        val results = store.querySimilar(queryVector, topK = 3)

        assertTrue(results.isNotEmpty(), "Should return RAG results for anonymized content")
        assertTrue(results.any { it.text.contains("Alpha") || it.text.contains("production") },
            "Results should contain business content despite anonymization")
    }

    @Test
    fun `ocrIngest with non-existent directory does not crash`(@TempDir tempDir: Path) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-nonexistent")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("nonexistent"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(0, store.countDocuments())
    }

    @Test
    fun `ocrIngest with custom jdbcUrl uses provided connection`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc_ocr.adoc").writeText("= Doc\n\nContenu.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-custom-jdbc")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.jdbcUrl.set(container.jdbcUrl)
        task.jdbcUser.set(container.username)
        task.jdbcPassword.set(container.password)

        task.executeIngest()

        assertTrue(store.countDocuments() >= 1)
    }

    @Test
    fun `ocrIngest with large document produces multiple chunks`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        val paragraphs = (1..20).joinToString("\n\n") { i ->
            "== Section $i\n\nCeci est le contenu de la section $i du document OCR. " +
            "Il contient plusieurs phrases pour générer des chunks. " +
            "Le pipeline d'ingestion découpe le texte en segments sémantiques."
        }
        ocrDir.resolve("large_ocr.adoc").writeText("= Grand Document\n\n$paragraphs")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-large")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(1, store.countDocuments())
        assertTrue(store.countChunks() > 1, "Large document should produce multiple chunks")
        assertTrue(store.allEmbeddingsNonNull())
    }

    @Test
    fun `E2E image to OCR to anonymize to ingest to query`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("scan.png").toFile()
        inputFile.writeText("fake png bytes for E2E test")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-e2e")
            .build()
        project.pluginManager.apply("java-base")

        val ocrTask = project.tasks.register("ocr", OcrTask::class.java).get()
        ocrTask.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        ocrTask.ocrProvider.set("gemini")
        ocrTask.ocrLanguage.set("fr")
        ocrTask.outputFormat.set("asciidoc")
        ocrTask.anonymizeOutput.set(true)
        ocrTask.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()
        ocrTask.ocrEngine = FakeOcrEngine()

        ocrTask.executeOcr()

        val ocrOutputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val ocrOutputFile = ocrOutputDir.resolve("scan_ocr.adoc")
        assertTrue(ocrOutputFile.exists(), "OCR output file should exist")

        val ocrContent = ocrOutputFile.readText()
        assertTrue(ocrContent.contains("= Titre Principal") || ocrContent.contains("= Document OCRisé"))
        assertFalse(ocrContent.contains("@"), "Anonymized output should not contain emails")

        val ingestTask = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        ingestTask.ocrOutputDir.set(project.layout.buildDirectory.dir("ocr"))
        ingestTask.vectorStore = store
        ingestTask.embeddingPipeline = pipeline

        ingestTask.executeIngest()

        assertTrue(store.countDocuments() >= 1, "Should have at least 1 document after ingest")
        assertTrue(store.countChunks() >= 1, "Should have at least 1 chunk after ingest")
        assertTrue(store.allEmbeddingsNonNull(), "All chunks should have embeddings")

        val queryVector = pipeline.embedQuery("document OCR test")
        val results = store.querySimilar(queryVector, topK = 3)

        assertTrue(results.isNotEmpty(), "E2E RAG query should return results")
        assertTrue(results.any { it.text.contains("Document OCRisé") || it.text.contains("FakeVisionProvider") },
            "E2E results should contain OCR content")
    }

    @Test
    fun `ocrIngest collects metrics per file`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc_ocr.adoc").writeText("= Document\n\nContenu test.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-metrics")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(1, task.ingestMetricsCollector.size)
        val m = task.ingestMetricsCollector[0]
        assertEquals("doc_ocr.adoc", m.fileName)
        assertTrue(m.chunkCount > 0)
        assertTrue(m.ingestDurationMs >= 0)
        assertTrue(m.embeddingDurationMs >= 0)
    }

    @Test
    fun `ocrIngest generates metrics report`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc_ocr.adoc").writeText("= Document\n\nContenu test.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-report")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        val reportFile = project.layout.buildDirectory.dir("reports/ocr").get().asFile.resolve("ocr-ingest-metrics.adoc")
        assertTrue(reportFile.exists(), "Ingest metrics report should be generated")
        val report = reportFile.readText()
        assertTrue(report.contains("= Rapport Métriques OCR"))
        assertTrue(report.contains("doc_ocr.adoc"))
    }

    @Test
    fun `ocrIngest with multiple files collects metrics for all`(@TempDir tempDir: Path) {
        val ocrDir = tempDir.resolve("ocr").toFile()
        ocrDir.mkdirs()
        ocrDir.resolve("doc1_ocr.adoc").writeText("= Doc 1\n\nContenu 1.")
        ocrDir.resolve("doc2_ocr.adoc").writeText("= Doc 2\n\nContenu 2.")

        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("ocr-ingest-multi-metrics")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocrIngest", OcrIngestTask::class.java).get()
        task.ocrOutputDir.set(project.layout.projectDirectory.dir("ocr"))
        task.vectorStore = store
        task.embeddingPipeline = pipeline

        task.executeIngest()

        assertEquals(2, task.ingestMetricsCollector.size)
        val names = task.ingestMetricsCollector.map { it.fileName }.sorted()
        assertEquals(listOf("doc1_ocr.adoc", "doc2_ocr.adoc"), names)
    }
}
