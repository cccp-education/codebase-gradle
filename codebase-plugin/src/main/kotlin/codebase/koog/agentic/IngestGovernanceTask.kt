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
import contracts.vibecoding.registry.EnforcementHook
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

    @get:Internal
    var lastExecutor: AgenticExecutor? = null
        private set

    /**
     * Hook de blocage utilisable par [contracts.vibecoding.registry.ToolRegistry].
     * Retourne `null` tant que l'ingestion n'a pas produit d'executables PRE_HOOK.
     */
    fun buildEnforcementHook(): EnforcementHook? {
        val executor = lastExecutor ?: return null
        return { toolName, arguments ->
            val result = executor.check(toolName, arguments)
            if (result.allowed) null else formatEnforcementResult(result)
        }
    }

    private fun formatEnforcementResult(result: ExecutionResult): String =
        listOfNotNull(
            result.reason,
            result.ruleId?.let { "ruleId=$it" }
        ).joinToString(" | ")

    init {
        group = "generate"
        description = "Ingest governance EAGER files (AGENT.adoc, INDEX.adoc, BACKLOG.adoc) into AgenticIngestor (in-memory stub)"
    }

    @TaskAction
    fun executeIngest() {
        val root = workspaceRoot.asFile.getOrNull()
            ?: error("workspaceRoot must be set")
        val output = outputFile.asFile.orNull
        executeIngest(root, output)
    }

    /**
     * Point d'entrée DDD indépendant des propriétés Gradle.
     * Permet l'appel programmatique depuis [GovernanceEnforcementWirer] et les tests.
     */
    fun executeIngest(root: File, output: File?) {
        val result = GovernanceIngestor(chunkValidator).ingest(root)
        lastIngestionReport = result.report
        lastExecutor = result.executor

        log.info(
            "[IngestGovernance] Report — scanned={}, added={}, skipped={}, modified={}, compiled={}",
            result.report.filesScanned, result.report.chunksAdded, result.report.chunksSkipped,
            result.report.chunksModified, result.report.artifactsCompiled
        )
        if (result.report.sectionsAdded.isNotEmpty()) {
            log.info("[IngestGovernance] Sections added: {}", result.report.sectionsAdded)
        }
        if (result.report.sectionsTotal.isNotEmpty()) {
            log.info("[IngestGovernance] Sections total: {}", result.report.sectionsTotal)
        }

        if (output != null) {
            output.parentFile.mkdirs()
            output.writeText(buildReportJson(result.report), Charsets.UTF_8)
            log.info("[IngestGovernance] Report written to {}", output.absolutePath)
        }
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
        appendLine("  \"validationErrors\": ${validationErrorsToJson(report.validationErrors)},")
        appendLine("  \"invalidChunks\": ${invalidChunksToJson(report.invalidChunks)}")
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

    private fun invalidChunksToJson(invalidChunks: List<InvalidChunk>): String = buildString {
        append("[")
        invalidChunks.withIndex().forEach { (index, invalidChunk) ->
            append("{")
            append("\"id\": \"${escapeJson(invalidChunk.id)}\", ")
            append("\"sourceFile\": \"${escapeJson(invalidChunk.sourceFile)}\", ")
            append("\"sourceLines\": \"${escapeJson(invalidChunk.sourceLines)}\", ")
            append("\"content\": \"${escapeJson(invalidChunk.content)}\", ")
            append("\"quarantinedAt\": \"${invalidChunk.quarantinedAt}\", ")
            append("\"errors\": ${validationErrorsToJson(invalidChunk.errors)}")
            append("}")
            if (index < invalidChunks.size - 1) append(", ")
        }
        append("]")
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