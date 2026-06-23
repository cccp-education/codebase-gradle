package codebase.koog.agentic

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Registre dynamiquement les artefacts agentiques compilés comme des tâches Gradle.
 *
 * Responsabilité unique : à partir d'une liste d'[ExecutableArtifact], créer
 * des tâches Gradle nommées de manière stable dans le projet courant.
 *
 * Types supportés :
 * - [ArtifactType.GRADLE_TASK]     → tâche `runProcedure_<id>`
 * - [ArtifactType.CONSTRAINT_CHECK] → tâche `enforceRule_<id>`
 * - [ArtifactType.VALIDATION]       → tâche `validate_<id>`
 */
class AgenticGradleTaskRegistrar {

    fun register(project: Project, executables: List<ExecutableArtifact>): List<String> {
        val registered = mutableListOf<String>()

        for (executable in executables) {
            val artifactType = executable.compiledArtifact.artifactType
            if (!isRegisterable(artifactType)) continue

            val taskName = computeTaskName(executable)
            if (project.tasks.findByName(taskName) != null) continue

            project.tasks.register(taskName, GovernanceExecutableTask::class.java) { task ->
                task.group = GOVERNANCE_GROUP
                task.description = executable.compiledArtifact.description
                task.markerFile.set(project.layout.buildDirectory.file("governance-tasks/$taskName.done"))
            }

            registered.add(taskName)
        }

        return registered
    }

    private fun isRegisterable(artifactType: ArtifactType): Boolean =
        artifactType == ArtifactType.GRADLE_TASK ||
            artifactType == ArtifactType.CONSTRAINT_CHECK ||
            artifactType == ArtifactType.VALIDATION

    private fun computeTaskName(executable: ExecutableArtifact): String {
        val prefix = when (executable.compiledArtifact.artifactType) {
            ArtifactType.GRADLE_TASK -> "runProcedure"
            ArtifactType.CONSTRAINT_CHECK -> "enforceRule"
            ArtifactType.VALIDATION -> "validate"
            else -> "governance"
        }
        val shortId = executable.compiledArtifact.sourceChunkId
            .take(ID_TRUNCATION)
            .replace(Regex("[^a-zA-Z0-9]"), "")
        return "${prefix}_$shortId"
    }

    abstract class GovernanceExecutableTask : DefaultTask() {

        private val log = LoggerFactory.getLogger(GovernanceExecutableTask::class.java)

        @get:OutputFile
        abstract val markerFile: RegularFileProperty

        @TaskAction
        fun execute() {
            val file = markerFile.get().asFile
            file.parentFile.mkdirs()
            file.writeText("executed at ${Instant.now()}")
            log.info("[GovernanceTask] executed: ${path}")
        }
    }

    private companion object {
        const val GOVERNANCE_GROUP = "governance"
        const val ID_TRUNCATION = 12
    }
}
