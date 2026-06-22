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
            governanceOntologizer = GovernanceOntologizer()
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
        appendLine("  \"artifactsCompiled\": ${report.artifactsCompiled},")
        appendLine("  \"sectionsAdded\": ${mapToJson(report.sectionsAdded)},")
        appendLine("  \"sectionsTotal\": ${mapToJson(report.sectionsTotal)}")
        appendLine("}")
    }

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