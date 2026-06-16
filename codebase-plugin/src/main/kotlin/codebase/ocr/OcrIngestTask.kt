package codebase.ocr

import codebase.rag.ChunkTokenizer
import codebase.rag.EmbeddingPipeline
import codebase.rag.PgVectorConfig
import codebase.rag.VectorStore
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Ingestion pgvector — dépend de l'état de la base externe")
abstract class OcrIngestTask : DefaultTask() {

    @get:InputDirectory
    @get:Optional
    @get:Option(option = "ocrOutputDir", description = "Répertoire contenant les fichiers OCR (défaut : build/ocr/)")
    abstract val ocrOutputDir: DirectoryProperty

    @get:Input
    @get:Optional
    @get:Option(option = "jdbcUrl", description = "URL JDBC pgvector (défaut : env PGVECTOR_JDBC_URL)")
    abstract val jdbcUrl: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "jdbcUser", description = "Utilisateur pgvector (défaut : env PGVECTOR_USER)")
    abstract val jdbcUser: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "jdbcPassword", description = "Mot de passe pgvector (défaut : env PGVECTOR_PASSWORD)")
    abstract val jdbcPassword: Property<String>

    @get:Internal
    var vectorStore: VectorStore? = null

    @get:Internal
    var embeddingPipeline: EmbeddingPipeline? = null

    @get:Internal
    val ingestMetricsCollector: MutableList<OcrMetrics> = mutableListOf()

    init {
        group = "collect"
        description = "Ingère les fichiers OCR dans pgvector (chunk → embedding → RAG)"
    }

    @TaskAction
    fun executeIngest() {
        val store = vectorStore ?: resolveStore()
        val pipeline = embeddingPipeline ?: EmbeddingPipeline(store)

        store.ensureSchema()

        val ocrDir = if (ocrOutputDir.isPresent) {
            ocrOutputDir.get().asFile
        } else {
            project.layout.buildDirectory.dir("ocr").get().asFile
        }

        if (!ocrDir.exists() || !ocrDir.isDirectory) {
            logger.warn("[ocrIngest] Répertoire OCR introuvable : {}", ocrDir.absolutePath)
            return
        }

        val ocrFiles = ocrDir.listFiles()?.filter { it.isFile && it.extension == "adoc" }?.sortedBy { it.name }
            ?: emptyList()

        if (ocrFiles.isEmpty()) {
            logger.lifecycle("[ocrIngest] Aucun fichier .adoc trouvé dans {}", ocrDir.absolutePath)
            return
        }

        logger.lifecycle("[ocrIngest] {} fichier(s) OCR à ingérer", ocrFiles.size)

        var totalChunks = 0
        for (file in ocrFiles) {
            val ingestStart = System.currentTimeMillis()
            val text = file.readText(Charsets.UTF_8)
            val chunks = ChunkTokenizer.splitIntoSentenceLevelChunks(text)
            store.insertDocument(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSize = file.length(),
                chunks = chunks,
                packageName = null,
                className = null,
                repoName = "ocr"
            )
            val ingestDuration = System.currentTimeMillis() - ingestStart
            totalChunks += chunks.size
            logger.lifecycle("[ocrIngest] {} : {} chunks, durée={}", file.name, chunks.size,
                OcrMetricsCalculator.formatDurationMs(ingestDuration))

            val metrics = OcrMetricsCalculator.buildMetrics(
                fileName = file.name,
                fileSizeBytes = file.length(),
                isImage = false,
                provider = "ingest",
                model = "pgvector",
                language = "n/a",
                ocrDurationMs = 0,
                outputLengthChars = text.length,
                anonymizationReplacements = 0,
                anonymizationCategories = emptyList()
            )
            ingestMetricsCollector.add(OcrMetricsCalculator.mergeIngestMetrics(
                metrics, chunkCount = chunks.size, ingestDurationMs = ingestDuration, embeddingDurationMs = 0
            ))
        }

        logger.lifecycle("[ocrIngest] {} documents, {} chunks insérés", ocrFiles.size, totalChunks)

        if (totalChunks > 0) {
            val embedStart = System.currentTimeMillis()
            val allRecords = store.fetchAllChunks()
            logger.lifecycle("[ocrIngest] Calcul des embeddings ONNX pour {} chunks...", allRecords.size)
            pipeline.embedAll(allRecords)
            val embedDuration = System.currentTimeMillis() - embedStart
            logger.lifecycle("[ocrIngest] Embeddings calculés et stockés en {}",
                OcrMetricsCalculator.formatDurationMs(embedDuration))

            ingestMetricsCollector.forEachIndexed { i, m ->
                ingestMetricsCollector[i] = m.copy(embeddingDurationMs = embedDuration / ocrFiles.size)
            }
        }

        logger.lifecycle(
            "[ocrIngest] Terminé — {} documents, {} chunks indexés",
            store.countDocuments(),
            store.countChunks()
        )

        if (ingestMetricsCollector.isNotEmpty()) {
            val report = OcrMetricsReport.generateAsciiDoc(ingestMetricsCollector.toList())
            val reportDir = project.layout.buildDirectory.dir("reports/ocr").get().asFile
            reportDir.mkdirs()
            val reportFile = File(reportDir, "ocr-ingest-metrics.adoc")
            reportFile.writeText(report, Charsets.UTF_8)
            logger.lifecycle("[ocrIngest] Rapport métriques écrit dans : {}", reportFile.absolutePath)
        }
    }

    private fun resolveStore(): VectorStore {
        val url = jdbcUrl.orNull ?: System.getenv("PGVECTOR_JDBC_URL") ?: "jdbc:postgresql://localhost:5432/codebase_rag"
        val user = jdbcUser.orNull ?: System.getenv("PGVECTOR_USER") ?: "codebase"
        val password = jdbcPassword.orNull ?: System.getenv("PGVECTOR_PASSWORD") ?: "codebase"
        return VectorStore(url, user, password)
    }
}
