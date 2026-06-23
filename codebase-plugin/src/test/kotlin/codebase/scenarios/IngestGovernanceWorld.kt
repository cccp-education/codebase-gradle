package codebase.scenarios

import codebase.koog.agentic.ChunkValidator
import codebase.koog.agentic.GovernanceSection
import codebase.koog.agentic.IngestGovernanceTask
import codebase.koog.agentic.IngestionReport
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files

class IngestGovernanceWorld {

    private val tempDir: File = Files.createTempDirectory("ingest-governance-world").toFile()

    var lastReport: IngestionReport? = null
        private set

    var lastOutputFile: File? = null
        private set

    var lastProject: Project? = null
        private set

    var lastRegisteredTaskNames: List<String> = emptyList()
        private set

    var chunkValidator: ChunkValidator = ChunkValidator()

    fun file(path: String): File = File(tempDir, path)

    fun writeFile(path: String, content: String) {
        val target = file(path)
        target.parentFile?.takeIf { it != tempDir }?.mkdirs()
        target.writeText(content)
    }

    fun runTask(outputPath: String? = null) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = outputPath?.let { file(it) }
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.chunkValidator = chunkValidator
            if (outputFile != null) {
                it.outputFile.set(outputFile)
            }
        }.get()
        task.executeIngest()
        lastReport = task.lastIngestionReport
        lastOutputFile = outputFile
        lastProject = project
        lastRegisteredTaskNames = task.lastRegisteredTaskNames
    }

    fun outputJson(): String {
        val out = lastOutputFile ?: error("No output file set")
        return out.readText()
    }

    fun reset() {
        lastReport = null
        lastOutputFile = null
        lastProject = null
        lastRegisteredTaskNames = emptyList()
        tempDir.deleteRecursively()
        tempDir.mkdirs()
    }

    fun assertSectionAdded(section: GovernanceSection) {
        val report = lastReport ?: error("No ingestion report")
        assert(report.sectionsAdded.containsKey(section)) {
            "Expected section $section in sectionsAdded, got ${report.sectionsAdded.keys}"
        }
    }

    fun assertSectionTotal(section: GovernanceSection) {
        val report = lastReport ?: error("No ingestion report")
        assert(report.sectionsTotal.containsKey(section)) {
            "Expected section $section in sectionsTotal, got ${report.sectionsTotal.keys}"
        }
    }

    fun assertSectionTotalGreaterThan(sectionName: String, min: Int) {
        val report = lastReport ?: error("No ingestion report")
        val section = GovernanceSection.valueOf(sectionName)
        val count = report.sectionsTotal[section] ?: 0
        assert(count > min) {
            "Expected section $section total > $min, got $count"
        }
    }
}
