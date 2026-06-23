package codebase.koog.agentic

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.slf4j.LoggerFactory
import java.io.File

@DisableCachingByDefault(because = "Ingest governance — in-memory stub, non-deterministic chunk ids")
abstract class IngestGovernanceTask : DefaultTask() {

    private val log = LoggerFactory.getLogger(IngestGovernanceTask::class.java)

    @get:Internal
    abstract val workspaceRoot: RegularFileProperty

    @get:OutputFile
    @get:Optional
    @get:Option(option = "outputFile", description = "Fichier de sortie rapport ingestion JSON")
    abstract val outputFile: RegularFileProperty

    @get:Internal
    var lastIngestionReport: IngestionReport? = null

    @get:Internal
    var chunkValidator: ChunkValidator = ChunkValidator()
        internal set

    init {
        group = "generate"
        description = "Ingest governance EAGER files (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) into AgenticIngestor (in-memory stub)"
    }

    @TaskAction
    fun executeIngest() {
        val root = workspaceRoot.asFile.getOrNull()
            ?: error("workspaceRoot must be set")

        val files = collectGovernanceFiles(root)
        log.info("[IngestGovernance] Collected {} governance files from {}", files.size, root.absolutePath)

        val repository = InMemoryAgenticChunkRepository()
        val ingestor = AgenticIngestor(
            repository = repository,
            governanceOntologizer = GovernanceOntologizer(),
            chunkValidator = chunkValidator
        )

        val report = runBlocking { ingestor.ingest(files) }
        lastIngestionReport = report

        log.info(
            "[IngestGovernance] Report — scanned={}, added={}, skipped={}, modified={}, compiled={}",
            report.filesScanned, report.chunksAdded, report.chunksSkipped,
            report.chunksModified, report.artifactsCompiled
        )
        if (report.sectionsAdded.isNotEmpty()) {
            log.info("[IngestGovernance] Sections added: {}", report.sectionsAdded)
        }
        if (report.sectionsTotal.isNotEmpty()) {
            log.info("[IngestGovernance] Sections total: {}", report.sectionsTotal)
        }

        if (outputFile.isPresent) {
            val out = outputFile.get().asFile
            out.parentFile.mkdirs()
            out.writeText(buildReportJson(report), Charsets.UTF_8)
            log.info("[IngestGovernance] Report written to {}", out.absolutePath)
        }
    }

    private fun collectGovernanceFiles(projectDir: File): List<Pair<String, String>> {
        val scanAgent = ScanAgent()
        return scanAgent.scan(projectDir).map { it.relativePath to it.content }
    }

    private fun buildReportJson(report: IngestionReport): String = buildString {
        appendLine("{")
        appendLine("  \"filesScanned\": ${report.filesScanned},")
        appendLine("  \"chunksAdded\": ${report.chunksAdded},")
        appendLine("  \"chunksSkipped\": ${report.chunksSkipped},")
        appendLine("  \"chunksModified\": ${report.chunksModified},")
        appendLine("  \"chunksInvalid\": ${report.chunksInvalid},")
        appendLine("  \"artifactsCompiled\": ${report.artifactsCompiled},")
        appendLine("  \"sectionsAdded\": ${mapToJson(report.sectionsAdded)},")
        appendLine("  \"sectionsTotal\": ${mapToJson(report.sectionsTotal)},")
        appendLine("  \"validationErrorsByType\": ${validationErrorsByTypeToJson(report.validationErrors)},")
        appendLine("  \"validationErrors\": ${validationErrorsToJson(report.validationErrors)}")
        appendLine("}")
    }

    private fun validationErrorsToJson(errors: List<ChunkValidationError>): String = buildString {
        append("[")
        errors.withIndex().forEach { (index, error) ->
            append("{")
            append("\"sourceFile\": \"${escapeJson(error.sourceFile)}\", ")
            append("\"sourceLines\": \"${escapeJson(error.sourceLines)}\", ")
            append("\"lineStart\": ${error.lineStart ?: "null"}, ")
            append("\"lineEnd\": ${error.lineEnd ?: "null"}, ")
            append("\"errorType\": \"${error.errorType.name}\", ")
            append("\"message\": \"${escapeJson(error.message)}\"")
            append("}")
            if (index < errors.size - 1) append(", ")
        }
        append("]")
    }

    private fun validationErrorsByTypeToJson(errors: List<ChunkValidationError>): String = buildString {
        val counts = errors.groupingBy { it.errorType }.eachCount()
        append("{")
        val entries = counts.entries.toList()
        for ((index, entry) in entries.withIndex()) {
            append("\"${entry.key.name}\": ${entry.value}")
            if (index < entries.size - 1) append(", ")
        }
        append("}")
    }

    private fun escapeJson(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun mapToJson(map: Map<GovernanceSection, Int>): String = buildString {
        append("{")
        val entries = map.entries.toList()
        for ((index, entry) in entries.withIndex()) {
            append("\"${entry.key.name}\": ${entry.value}")
            if (index < entries.size - 1) append(", ")
        }
        append("}")
    }
}