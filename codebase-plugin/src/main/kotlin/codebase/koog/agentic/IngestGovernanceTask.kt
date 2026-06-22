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
        val ingestor = AgenticIngestor(repository = repository)

        val report = runBlocking { ingestor.ingest(files) }
        lastIngestionReport = report

        log.info(
            "[IngestGovernance] Report — scanned={}, added={}, skipped={}, modified={}, compiled={}",
            report.filesScanned, report.chunksAdded, report.chunksSkipped,
            report.chunksModified, report.artifactsCompiled
        )

        if (outputFile.isPresent) {
            val out = outputFile.get().asFile
            out.parentFile.mkdirs()
            out.writeText(buildReportJson(report), Charsets.UTF_8)
            log.info("[IngestGovernance] Report written to {}", out.absolutePath)
        }
    }

    private fun collectGovernanceFiles(projectDir: File): List<Pair<String, String>> {
        val eagerRelativePaths = listOf(
            "AGENT.adoc",
            ".agents/INDEX.adoc",
            "PROMPT_REPRISE.adoc",
            ".agents/SESSIONS_HISTORY.adoc",
            ".agents/TEST_COVERAGE_ANALYSIS.adoc",
            "BACKLOG.adoc",
            ".agents/BACKLOG.adoc"
        )

        val result = mutableListOf<Pair<String, String>>()
        for (relativePath in eagerRelativePaths) {
            val rootCandidate = File(projectDir, relativePath)
            if (rootCandidate.exists() && rootCandidate.isFile) {
                result.add(relativePath to rootCandidate.readText(Charsets.UTF_8))
                continue
            }
            projectDir.listFiles()?.filter { it.isDirectory }?.forEach { subDir ->
                val subCandidate = File(subDir, relativePath)
                if (subCandidate.exists() && subCandidate.isFile) {
                    result.add("$subDir.name/$relativePath" to subCandidate.readText(Charsets.UTF_8))
                }
            }
        }
        return result
    }

    private fun buildReportJson(report: IngestionReport): String = buildString {
        appendLine("{")
        appendLine("  \"filesScanned\": ${report.filesScanned},")
        appendLine("  \"chunksAdded\": ${report.chunksAdded},")
        appendLine("  \"chunksSkipped\": ${report.chunksSkipped},")
        appendLine("  \"chunksModified\": ${report.chunksModified},")
        appendLine("  \"artifactsCompiled\": ${report.artifactsCompiled}")
        appendLine("}")
    }
}