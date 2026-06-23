package codebase.koog.agentic

import contracts.vibecoding.registry.ToolRegistry
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IngestGovernanceTaskTest {

    @Test
    fun `task should be registered in generate group`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java).get()

        assertEquals("ingestGovernance", task.name)
        assertEquals("generate", task.group)
        assertTrue(task.description?.contains("governance") == true)
    }

    @Test
    fun `task ingests AGENT adoc from workspace root`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            * NE DOIT JAMAIS committer sans permission
            * DOIT valider les tests avant fin de session
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertTrue(report != null, "IngestionReport should be set")
        assertTrue(report!!.filesScanned >= 1, "Should scan at least AGENT.adoc, got ${report.filesScanned}")
        assertTrue(report.chunksAdded > 0, "Should add chunks, got ${report.chunksAdded}")
    }

    @Test
    fun `task ingests governance files from subproject`(@TempDir tempDir: File) {
        val subproject = File(tempDir, "codebase-plugin").apply { mkdirs() }
        File(subproject, "AGENT.adoc").writeText("= Sub Agent\n\n* [ ] Item backlog\n")
        File(subproject, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport!!
        assertTrue(report.filesScanned >= 1, "Should scan subproject files, got ${report.filesScanned}")
        assertTrue(report.chunksAdded > 0, "Should add chunks from subproject, got ${report.chunksAdded}")
    }

    @Test
    fun `task produces empty report for empty workspace`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport!!
        assertEquals(0, report.filesScanned)
        assertEquals(0, report.chunksAdded)
    }

    @Test
    fun `task writes ingestion report to output file`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists(), "Report file should be written")
        val content = outputFile.readText()
        assertTrue(content.contains("filesScanned"))
        assertTrue(content.contains("chunksAdded"))
    }

    @Test
    fun `task report includes governance sections for recognized files`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "INDEX.adoc").writeText("= Index\n\n== EPIC Y\nEn cours.\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport!!
        assertTrue(report.sectionsAdded.containsKey(GovernanceSection.RULES_ABSOLUES),
            "Should add chunks to RULES_ABSOLUES")
        assertTrue(report.sectionsAdded.containsKey(GovernanceSection.ETAT_EPICS),
            "Should add chunks to ETAT_EPICS")
        assertTrue(report.sectionsTotal[GovernanceSection.RULES_ABSOLUES] ?: 0 > 0,
            "Should report total for RULES_ABSOLUES")
        assertTrue(report.sectionsTotal[GovernanceSection.ETAT_EPICS] ?: 0 > 0,
            "Should report total for ETAT_EPICS")
    }

    @Test
    fun `task report json includes sectionsAdded and sectionsTotal`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists(), "Report file should be written")
        val content = outputFile.readText()
        assertTrue(content.contains("RULES_ABSOLUES"), "JSON should contain RULES_ABSOLUES")
        assertTrue(content.contains("sectionsAdded"), "JSON should contain sectionsAdded")
        assertTrue(content.contains("sectionsTotal"), "JSON should contain sectionsTotal")
    }

    @Test
    fun `task report json exposes typed validation errors`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val base = super.validate(chunk)
                if (base.valid) {
                    val error = ChunkValidationError(
                        sourceFile = chunk.sourceFile,
                        sourceLines = chunk.sourceLines,
                        lineStart = 1,
                        lineEnd = 1,
                        errorType = ChunkValidationErrorType.MISSING_CONTENT,
                        message = "injected validation error"
                    )
                    return ValidationResult(valid = false, errors = listOf(error))
                }
                return base
            }
        }
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
            it.chunkValidator = fakeValidator
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists(), "Report file should be written")
        val content = outputFile.readText()
        assertTrue(content.contains("\"errorType\": \"MISSING_CONTENT\""), "JSON should expose errorType")
        assertTrue(content.contains("\"lineStart\": 1"), "JSON should expose lineStart")
        assertTrue(content.contains("\"lineEnd\": 1"), "JSON should expose lineEnd")
        assertTrue(content.contains("\"validationErrorsByType\""), "JSON should expose validationErrorsByType")
        val summaryRegex = Regex("\"validationErrorsByType\"\\s*:\\s*\\{[^}]*\"MISSING_CONTENT\"\\s*:\\s*\\d+")
        assertTrue(summaryRegex.containsMatchIn(content), "JSON should summarize errors by type")
    }

    @Test
    fun `buildEnforcementHook returns null when no ingestion happened`(@TempDir tempDir: File) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java).get()

        assertNull(task.buildEnforcementHook(), "Hook should be null before ingestion")
    }

    @Test
    fun `buildEnforcementHook blocks exec_shell after ingestion of INTERDIRE rule`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            * NE DOIT JAMAIS git push sans permission explicite.
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val hook = task.buildEnforcementHook()
        assertNotNull(hook, "Hook should be available after ingestion")
        val reason = hook("exec_shell", mapOf("command" to "git push origin main"))
        assertNotNull(reason, "Hook should block git push")
        assertTrue(reason.contains("push", ignoreCase = true), "Reason should mention push: $reason")
    }

    @Test
    fun `buildEnforcementHook wired into ToolRegistry blocks exec_gradle publish`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            * NE DOIT JAMAIS ./gradlew publish sans verification.
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val registry = ToolRegistry(enforcementHook = task.buildEnforcementHook())

        val exception = assertThrows<SecurityException> {
            registry.execute("exec_gradle", mapOf("task" to "publish"), workspaceRoot = tempDir.absolutePath)
        }

        assertTrue(exception.message!!.contains("ENFORCEMENT BLOCKED"))
        assertTrue(exception.message!!.contains("publish", ignoreCase = true))
    }

    @Test
    fun `V-9_15 invalid chunks are quarantined in ingestion report`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val error = ChunkValidationError(
                    sourceFile = chunk.sourceFile,
                    sourceLines = chunk.sourceLines,
                    lineStart = 1,
                    lineEnd = 1,
                    errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                    message = "injected validation error"
                )
                return ValidationResult(valid = false, errors = listOf(error))
            }
        }

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.chunkValidator = fakeValidator
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertNotNull(report)
        assertTrue(report!!.chunksInvalid > 0, "Should count invalid chunks")
        assertEquals(report.chunksInvalid, report.invalidChunks.size, "invalidChunks list size should match count")
        assertTrue(report.invalidChunks.all { it.errors.isNotEmpty() }, "Each quarantined chunk should carry errors")
    }

    @Test
    fun `V-9_15 valid chunks are not quarantined`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertNotNull(report)
        assertEquals(0, report!!.invalidChunks.size, "Valid chunks should not be quarantined")
    }

    @Test
    fun `V-9_15 report json includes invalidChunks array`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val error = ChunkValidationError(
                    sourceFile = chunk.sourceFile,
                    sourceLines = chunk.sourceLines,
                    lineStart = 1,
                    lineEnd = 1,
                    errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                    message = "injected validation error"
                )
                return ValidationResult(valid = false, errors = listOf(error))
            }
        }

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
            it.chunkValidator = fakeValidator
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("\"invalidChunks\""), "JSON should expose invalidChunks")
        assertTrue(content.contains("\"quarantinedAt\""), "JSON should expose quarantinedAt")
        assertTrue(content.contains("\"errorType\": \"CHECKSUM_MISMATCH\""), "JSON should expose quarantine error type")
    }

    @Test
    fun `V-9_16 strict validation is disabled by default and does not fail on invalid chunks`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val error = ChunkValidationError(
                    sourceFile = chunk.sourceFile,
                    sourceLines = chunk.sourceLines,
                    lineStart = 1,
                    lineEnd = 1,
                    errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                    message = "injected validation error"
                )
                return ValidationResult(valid = false, errors = listOf(error))
            }
        }

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.chunkValidator = fakeValidator
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertNotNull(report)
        assertTrue(report!!.invalidChunks.isNotEmpty(), "Invalid chunks should be quarantined")
    }

    @Test
    fun `V-9_16 strict validation fails when invalid chunks are detected`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val fakeValidator = object : ChunkValidator() {
            override fun validate(chunk: AgenticChunk): ValidationResult {
                val error = ChunkValidationError(
                    sourceFile = chunk.sourceFile,
                    sourceLines = chunk.sourceLines,
                    lineStart = 1,
                    lineEnd = 1,
                    errorType = ChunkValidationErrorType.CHECKSUM_MISMATCH,
                    message = "injected validation error"
                )
                return ValidationResult(valid = false, errors = listOf(error))
            }
        }

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.chunkValidator = fakeValidator
            it.strictValidation.set(true)
        }.get()

        val exception = assertThrows<org.gradle.api.GradleException> {
            task.executeIngest()
        }

        assertTrue(exception.message!!.contains("strict validation"), "Exception should mention strict validation")
        assertTrue(exception.message!!.contains("CHECKSUM_MISMATCH"), "Exception should mention error type")
    }

    @Test
    fun `V-9_16 strict validation passes when all chunks are valid`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.strictValidation.set(true)
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertNotNull(report)
        assertTrue(report!!.invalidChunks.isEmpty(), "No invalid chunks expected")
    }

    @Test
    fun `V-9_18 registers Gradle tasks for GENERER procedure chunks`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            == Generation Procedure
            . GENERER le scenario pedagogique global
            . Produire le document AsciiDoc
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val names = task.lastRegisteredTaskNames
        assertTrue(names.isNotEmpty(), "Should register at least one governance task")
        assertTrue(names.all { it.startsWith("runProcedure_") }, "All registered tasks should be runProcedure_*")
        names.forEach { name ->
            assertNotNull(project.tasks.findByName(name), "Task $name should exist")
        }
    }

    @Test
    fun `V-9_18 registered governance task is executable and writes marker`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            == Check Procedure
            . GENERER le rapport de synthese
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val names = task.lastRegisteredTaskNames
        assertTrue(names.isNotEmpty())
        val taskName = names.first()
        val registered = project.tasks.getByName(taskName)
        assertEquals("governance", registered.group)

        project.gradle.projectsEvaluated {
            project.tasks.getByName(taskName).actions.forEach { it.execute(registered) }
        }
    }

    @Test
    fun `V-9_18 registers enforceRule task for CONSTRAINT chunks`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText(
            """
            = Agent

            == Limits
            Maximum 50k tokens EAGER (~3000 lignes).
            """.trimIndent()
        )

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        val names = task.lastRegisteredTaskNames
        assertTrue(names.any { it.startsWith("enforceRule_") }, "Should register enforceRule_*")
    }

    @Test
    fun `V-9_19 incremental mode first run ingests all files`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.governanceConfig.set(GovernanceSummaryConfig(incremental = true))
        }.get()

        task.executeIngest()

        val report = task.lastIngestionReport
        assertNotNull(report)
        assertTrue(report!!.filesScanned >= 2, "First incremental run should ingest all files")
        assertTrue(report.chunksAdded > 0, "First incremental run should add chunks")
        val incremental = task.lastIncrementalReport
        assertNotNull(incremental, "Incremental report should be populated in incremental mode")
        assertTrue(incremental.added.contains("AGENT.adoc"), "AGENT.adoc should be added on first run")
        assertTrue(incremental.added.contains("BACKLOG.adoc"), "BACKLOG.adoc should be added on first run")
        assertTrue(incremental.unchanged.isEmpty(), "No unchanged on first run")
    }

    @Test
    fun `V-9_19 incremental mode second run without changes skips all files`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.governanceConfig.set(GovernanceSummaryConfig(incremental = true))
        }.get()

        task.executeIngest()
        val firstReport = task.lastIngestionReport!!
        assertTrue(firstReport.chunksAdded > 0, "First run should add chunks")

        task.executeIngest()
        val secondReport = task.lastIngestionReport!!
        assertEquals(0, secondReport.filesScanned, "Second run should scan 0 files")
        assertEquals(0, secondReport.chunksAdded, "Second run should add 0 chunks")

        val incremental = task.lastIncrementalReport
        assertNotNull(incremental)
        assertTrue(incremental.added.isEmpty(), "No added on second unchanged run")
        assertTrue(incremental.modified.isEmpty(), "No modified on second unchanged run")
        assertTrue(incremental.removed.isEmpty(), "No removed on second unchanged run")
        assertTrue(
            incremental.unchanged.contains("AGENT.adoc"),
            "AGENT.adoc should be unchanged on second run"
        )
        assertTrue(
            incremental.skippedDueToIncremental.contains("AGENT.adoc"),
            "AGENT.adoc should be skipped due to incremental"
        )
    }

    @Test
    fun `V-9_19 incremental mode detects modified file and reingests only it`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.governanceConfig.set(GovernanceSummaryConfig(incremental = true))
        }.get()

        task.executeIngest()
        assertTrue(task.lastIngestionReport!!.chunksAdded > 0, "First run should add chunks")

        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n* DOIT valider les tests\n")
        task.executeIngest()

        val secondReport = task.lastIngestionReport!!
        assertEquals(1, secondReport.filesScanned, "Second run should only ingest the modified file")
        assertTrue(secondReport.chunksAdded > 0, "Modified file should produce new chunks")

        val incremental = task.lastIncrementalReport
        assertNotNull(incremental)
        assertEquals(listOf("AGENT.adoc"), incremental.modified, "Only AGENT.adoc should be modified")
        assertTrue(incremental.unchanged.contains("BACKLOG.adoc"), "BACKLOG.adoc should be unchanged")
        assertTrue(incremental.added.isEmpty(), "No added on second run")
    }

    @Test
    fun `V-9_19 incremental mode detects removed file`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.governanceConfig.set(GovernanceSummaryConfig(incremental = true))
        }.get()

        task.executeIngest()

        File(tempDir, "BACKLOG.adoc").delete()
        task.executeIngest()

        val incremental = task.lastIncrementalReport
        assertNotNull(incremental)
        assertTrue(incremental.removed.contains("BACKLOG.adoc"), "BACKLOG.adoc should be removed")
        assertTrue(incremental.unchanged.contains("AGENT.adoc"), "AGENT.adoc should be unchanged")
    }

    @Test
    fun `V-9_19 incremental mode detects newly added file`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.governanceConfig.set(GovernanceSummaryConfig(incremental = true))
        }.get()

        task.executeIngest()

        File(tempDir, "INDEX.adoc").writeText("= Index\n\n== EPIC Y\nEn cours.\n")
        task.executeIngest()

        val secondReport = task.lastIngestionReport!!
        assertEquals(1, secondReport.filesScanned, "Second run should only ingest the added file")

        val incremental = task.lastIncrementalReport
        assertNotNull(incremental)
        assertTrue(incremental.added.contains("INDEX.adoc"), "INDEX.adoc should be added")
        assertTrue(incremental.unchanged.contains("AGENT.adoc"), "AGENT.adoc should be unchanged")
    }

    @Test
    fun `V-9_19 incremental report is exposed in output JSON`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")
        File(tempDir, "BACKLOG.adoc").writeText("= Backlog\n\n* [ ] Open item\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val outputFile = File(tempDir, "ingestion-report.json")
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
            it.outputFile.set(outputFile)
            it.governanceConfig.set(GovernanceSummaryConfig(incremental = true))
        }.get()

        task.executeIngest()

        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("\"incremental\""), "JSON should expose incremental section")
        assertTrue(content.contains("\"added\""), "JSON should expose added files")
        assertTrue(content.contains("AGENT.adoc"), "JSON should list AGENT.adoc as added")
    }

    @Test
    fun `V-9_19 non-incremental mode keeps legacy behavior with no incremental report`(@TempDir tempDir: File) {
        File(tempDir, "AGENT.adoc").writeText("= Agent\n\n* NE DOIT JAMAIS leak de secrets\n")

        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        val task = project.tasks.register("ingestGovernance", IngestGovernanceTask::class.java) {
            it.workspaceRoot.set(project.layout.projectDirectory.file("."))
        }.get()

        task.executeIngest()

        assertNull(task.lastIncrementalReport, "Incremental report should be null in non-incremental mode")
        val report = task.lastIngestionReport!!
        assertTrue(report.chunksAdded > 0, "Legacy mode should ingest all files")
    }
}