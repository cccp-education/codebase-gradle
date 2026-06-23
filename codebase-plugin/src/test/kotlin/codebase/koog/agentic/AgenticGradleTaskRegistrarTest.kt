package codebase.koog.agentic

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AgenticGradleTaskRegistrarTest {

    private val registrar = AgenticGradleTaskRegistrar()

    private fun executable(
        id: String = "chunk-abc123",
        artifactType: ArtifactType = ArtifactType.GRADLE_TASK,
        description: String = "Run procedure",
        payload: ArtifactPayload = ArtifactPayload.GradleTaskPayload(
            taskName = "verifyBuild",
            description = description
        )
    ): ExecutableArtifact {
        val compiled = CompiledArtifact(
            sourceChunkId = id,
            artifactType = artifactType,
            description = description,
            targetHint = "codebase",
            confidence = 0.85,
            payload = payload
        )
        return ExecutableArtifact(compiled, payload)
    }

    @Test
    fun `should register GRADLE_TASK artifact as runProcedure task`() {
        val project = ProjectBuilder.builder().build()

        val registered = registrar.register(project, listOf(executable()))

        assertEquals(1, registered.size)
        val task = project.tasks.findByName(registered.first())
        assertNotNull(task)
        assertTrue(task.name.startsWith("runProcedure_"))
        assertEquals("governance", task.group)
    }

    @Test
    fun `should register CONSTRAINT_CHECK artifact as enforceRule task`() {
        val project = ProjectBuilder.builder().build()
        val payload = ArtifactPayload.ConstraintPayload(
            constraintDescription = "Max tokens",
            maxTokens = 50000
        )
        val constraint = executable(
            id = "constraint-xyz",
            artifactType = ArtifactType.CONSTRAINT_CHECK,
            description = "Max tokens",
            payload = payload
        )

        val registered = registrar.register(project, listOf(constraint))

        assertEquals(1, registered.size)
        assertTrue(registered.first().startsWith("enforceRule_"))
    }

    @Test
    fun `should register VALIDATION artifact as validate task`() {
        val project = ProjectBuilder.builder().build()
        val payload = ArtifactPayload.ValidationPayload(checkDescription = "Verify build")
        val validation = executable(
            id = "validation-001",
            artifactType = ArtifactType.VALIDATION,
            description = "Verify build",
            payload = payload
        )

        val registered = registrar.register(project, listOf(validation))

        assertEquals(1, registered.size)
        assertTrue(registered.first().startsWith("validate_"))
    }

    @Test
    fun `should skip non-registerable artifact types`() {
        val project = ProjectBuilder.builder().build()
        val metadata = executable(
            id = "meta-001",
            artifactType = ArtifactType.METADATA,
            description = "Metadata",
            payload = ArtifactPayload.MetadataPayload(metadataKey = "date", metadataValue = "2026-06-23")
        )

        val registered = registrar.register(project, listOf(metadata))

        assertTrue(registered.isEmpty())
    }

    @Test
    fun `should skip duplicate task names`() {
        val project = ProjectBuilder.builder().build()
        val first = executable(id = "chunk-abc123")
        val second = executable(id = "chunk-abc123")

        registrar.register(project, listOf(first))
        val registered = registrar.register(project, listOf(second))

        assertTrue(registered.isEmpty())
    }

    @Test
    fun `registered task writes marker on execution`() {
        val project = ProjectBuilder.builder().build()
        val exec = executable(id = "proc-001")

        val names = registrar.register(project, listOf(exec))
        val task = project.tasks.getByName(names.first()) as AgenticGradleTaskRegistrar.GovernanceExecutableTask
        task.execute()

        assertTrue(task.markerFile.get().asFile.exists())
    }

    @Test
    fun `should register multiple executables at once`() {
        val project = ProjectBuilder.builder().build()
        val tasks = listOf(
            executable(id = "p1", artifactType = ArtifactType.GRADLE_TASK),
            executable(id = "c1", artifactType = ArtifactType.CONSTRAINT_CHECK),
            executable(id = "v1", artifactType = ArtifactType.VALIDATION)
        )

        val registered = registrar.register(project, tasks)

        assertEquals(3, registered.size)
        assertTrue(registered.any { it.startsWith("runProcedure_") })
        assertTrue(registered.any { it.startsWith("enforceRule_") })
        assertTrue(registered.any { it.startsWith("validate_") })
    }
}
