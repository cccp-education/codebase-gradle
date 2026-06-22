package codebase.scenarios

import codebase.CodebasePlugin
import codebase.koog.expert.ExpertDomain
import codebase.koog.expert.ExpertExposureTask
import codebase.koog.expert.ExpertRegistration
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Epic8ExpertExposureSteps(private val world: Epic8ExpertExposureWorld) {

    @Given("an expert exposure registry with {string} and {string} domains")
    fun `expert exposure registry with domains`(domain1: String, domain2: String) {
        world.projectDir = Files.createTempDirectory("epic8-cucumber").toFile()
        world.registry.registerAll(listOf(
            ExpertRegistration(ExpertDomain(domain1, "$domain1 domain"), "gpt-oss:120b-cloud", "http://localhost:11437", 120),
            ExpertRegistration(ExpertDomain(domain2, "$domain2 domain"), "gpt-oss:120b-cloud", "http://localhost:11438", 90)
        ))
    }

    @Given("the codebase plugin is applied for expert exposure")
    fun `codebase plugin applied for expert exposure`() {
        world.projectDir = Files.createTempDirectory("epic8-cucumber").toFile()
    }

    @When("I check for exposure task {string}")
    fun `check for exposure task`(taskName: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(world.projectDir!!)
            .withName("epic8-lookup")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        world.task = project.tasks.findByName(taskName) as? ExpertExposureTask
        world.taskGroup = world.task?.group
    }

    @When("I expose experts with anonymization disabled")
    fun `expose experts anonymization disabled`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(world.projectDir!!)
            .withName("epic8-expose")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = world.registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.anonymizeEndpoints.set(false)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        world.manifestContent = outputFile.readText()
    }

    @When("I expose experts with anonymization enabled")
    fun `expose experts anonymization enabled`() {
        val project = ProjectBuilder.builder()
            .withProjectDir(world.projectDir!!)
            .withName("epic8-anon")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = world.registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.anonymizeEndpoints.set(true)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        world.manifestContent = outputFile.readText()
    }

    @When("I expose experts with domains {string} and anonymization disabled")
    fun `expose experts with domains`(domains: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(world.projectDir!!)
            .withName("epic8-filter")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("exposeExperts", ExpertExposureTask::class.java).get()
        task.expertRegistry = world.registry
        task.outputFile.set(project.layout.buildDirectory.file("experts/exposure-manifest.json"))
        task.domains.set(domains.split(",").map { it.trim() })
        task.anonymizeEndpoints.set(false)

        task.executeExposure()

        val outputFile = project.layout.buildDirectory.file("experts/exposure-manifest.json").get().asFile
        world.manifestContent = outputFile.readText()
    }

    @Then("exposure task {string} should be registered")
    fun `exposure task should be registered`(taskName: String) {
        assertNotNull(world.task, "Task '$taskName' should be registered")
    }

    @Then("exposure task {string} should be in group {string}")
    fun `exposure task should be in group`(taskName: String, group: String) {
        assertNotNull(world.task, "Task '$taskName' should exist")
        assertEquals(group, world.taskGroup, "Task group mismatch")
    }

    @Then("the manifest file exists")
    fun `manifest file exists`() {
        assertNotNull(world.manifestContent, "Manifest content should not be null")
        assertTrue(world.manifestContent!!.isNotBlank(), "Manifest should not be empty")
    }

    @Then("the manifest contains domain {string}")
    fun `manifest contains domain`(domain: String) {
        assertTrue(world.manifestContent!!.contains("\"domain\": \"$domain\""),
            "Manifest should contain domain '$domain'. Got: ${world.manifestContent}")
    }

    @Then("the manifest contains model {string}")
    fun `manifest contains model`(model: String) {
        assertTrue(world.manifestContent!!.contains("\"modelName\": \"$model\""),
            "Manifest should contain model '$model'. Got: ${world.manifestContent}")
    }

    @Then("the manifest is valid JSON")
    fun `manifest is valid JSON`() {
        val content = world.manifestContent!!
        assertTrue(content.trimStart().startsWith("{"), "Should start with {")
        assertTrue(content.trimEnd().endsWith("}"), "Should end with }")
    }

    @Then("the manifest contains {string}")
    fun `manifest contains`(expected: String) {
        assertTrue(world.manifestContent!!.contains(expected),
            "Manifest should contain '$expected'. Got: ${world.manifestContent}")
    }

    @Then("the manifest does not contain {string}")
    fun `manifest does not contain`(forbidden: String) {
        assertTrue(!world.manifestContent!!.contains(forbidden),
            "Manifest should NOT contain '$forbidden'. Got: ${world.manifestContent}")
    }

    @Then("the manifest does not contain domain {string}")
    fun `manifest does not contain domain`(domain: String) {
        assertTrue(!world.manifestContent!!.contains("\"domain\": \"$domain\""),
            "Manifest should NOT contain domain '$domain'. Got: ${world.manifestContent}")
    }

    @After
    fun cleanup() {
        world.projectDir?.deleteRecursively()
    }
}
