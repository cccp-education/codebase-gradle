package codebase.scenarios

import codebase.CodebasePlugin
import codebase.koog.agentic.IngestGovernanceTask
import io.cucumber.java.Before
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EpicV917Steps {

    private lateinit var tempDir: Path
    private var extensionDsl: String = ""
    private var strictFromTask: Boolean = false

    @Before("@epic_v_9_17")
    fun reset() {
        tempDir = Files.createTempDirectory("epic-v-9-17-")
        extensionDsl = ""
        strictFromTask = false
    }

    @Given("a Gradle project with AGENT.adoc containing {string}")
    fun `agent adoc content`(content: String) {
        File(tempDir.toFile(), "AGENT.adoc").writeText("= Agent\n\n$content\n")
    }

    @Given("the gradle property {string} is set to {string}")
    fun `gradle property set`(key: String, value: String) {
        File(tempDir.toFile(), "gradle.properties").writeText("$key=$value")
    }

    @When("the plugin is applied with no governance configuration")
    fun `apply plugin no config`() {
        buildProjectAndCaptureStrict("")
    }

    @When("the plugin is applied with {string}")
    fun `apply plugin with dsl`(dsl: String) {
        extensionDsl = dsl
        buildProjectAndCaptureStrict(dsl)
    }

    @Then("the ingestGovernance task has strictValidation disabled")
    fun `strict validation disabled`() {
        assertFalse(strictFromTask, "Expected strictValidation to be disabled")
    }

    @Then("the ingestGovernance task has strictValidation enabled")
    fun `strict validation enabled`() {
        assertTrue(strictFromTask, "Expected strictValidation to be enabled")
    }

    private fun buildProjectAndCaptureStrict(dsl: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-v9-17-dsl")
            .build()

        project.plugins.apply(CodebasePlugin::class.java)
        if (dsl.isNotBlank()) {
            project.extensions.configure(codebase.koog.agentic.CodebaseGovernanceExtension::class.java) {
                it.strictValidation.set(dsl.contains("true"))
            }
        }

        val task = project.tasks.getByName("ingestGovernance") as IngestGovernanceTask
        strictFromTask = task.governanceConfig.get().strictValidation
    }
}
