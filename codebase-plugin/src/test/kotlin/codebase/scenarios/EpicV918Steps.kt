package codebase.scenarios

import codebase.koog.agentic.AgenticGradleTaskRegistrar
import io.cucumber.java.Before
import io.cucumber.java.en.Then
import kotlin.test.assertTrue

class EpicV918Steps(private val world: IngestGovernanceWorld) {

    @Before("@epic_v_9_18")
    fun reset() {
        world.reset()
    }

    @Then("the ingestion report has artifacts compiled greater than {int}")
    fun `artifacts compiled greater than`(min: Int) {
        val report = world.lastReport ?: error("No ingestion report")
        assertTrue(report.artifactsCompiled > min,
            "Expected artifactsCompiled > $min, got ${report.artifactsCompiled}")
    }

    @Then("a Gradle task named like {string} is registered")
    fun `gradle task named like is registered`(pattern: String) {
        val project = world.lastProject ?: error("No project captured")
        val prefix = pattern.replace("*", "")
        val matching = project.tasks.names.filter { it.contains(prefix) }
        assertTrue(matching.isNotEmpty(), "Expected a task matching $pattern, got ${project.tasks.names}")
    }

    @Then("the registered governance task can be executed")
    fun `registered governance task can be executed`() {
        val project = world.lastProject ?: error("No project captured")
        val names = world.lastRegisteredTaskNames
        assertTrue(names.isNotEmpty(), "No governance tasks registered")
        val name = names.first()
        val task = project.tasks.getByName(name) as AgenticGradleTaskRegistrar.GovernanceExecutableTask
        task.execute()
        assertTrue(task.markerFile.get().asFile.exists(), "Marker file should exist after execution")
    }
}
