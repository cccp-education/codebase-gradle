package codebase.scenarios

import codebase.koog.agentic.IngestionReport
import codebase.koog.agentic.ScanAgent
import codebase.koog.agentic.ScannedFile
import org.gradle.testfixtures.ProjectBuilder
import codebase.koog.agentic.IngestGovernanceTask
import java.io.File
import java.nio.file.Files

class ScanAgentWorld {

    private val scanAgent = ScanAgent()
    var scannedFiles: List<ScannedFile> = emptyList()
        private set
    var ingestionReport: IngestionReport? = null
        private set

    val workspaceDir: File = Files.createTempDirectory("scan-agent-world").toFile()

    fun file(path: String): File = File(workspaceDir, path)

    fun scan(dir: File = workspaceDir) {
        scannedFiles = scanAgent.scan(dir)
    }

    fun ingestWorkspace(dir: File = workspaceDir) {
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()
        task.executeIngest()
        ingestionReport = task.lastIngestionReport
    }

    fun reset() {
        scannedFiles = emptyList()
        ingestionReport = null
        workspaceDir.deleteRecursively()
        workspaceDir.mkdirs()
    }
}