package codebase.scenarios

import vibecoding.contracts.state.AugmentedState
import codebase.koog.KoogPlanningOrchestrator
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Step Definitions Cucumber pour le KoogPlanningOrchestrator.
 *
 * Pattern PicoContainer standardisé — aligné sur plantuml-gradle, slider-gradle,
 * bakery-gradle, readme-gradle : le World est injecté par constructeur.
 * PicoContainer crée une nouvelle instance par scénario → pas de @Before reset.
 */
class KoogPlanningOrchestratorSteps(private val world: KoogPlanningOrchestratorWorld) {

    // ── Given ──

    @Given("a KoogPlanningOrchestrator is instantiated")
    fun `koog planning orchestrator instantiated`() {
        assertNotNull(world.orchestrator, "Orchestrator should be lazily instantiated via PicoContainer world")
    }

    @Given("a temporary workspace root {string} is created for planning")
    fun `temporary workspace root created for planning`(path: String) {
        val dir = File(path).also { it.mkdirs() }
        assertTrue(dir.isDirectory, "Workspace root should be a directory: $path")
    }

    // ── When ──

    @When("I execute the planning pipeline with intention {string}")
    fun `execute planning pipeline`(intention: String) {
        world.intention = intention

        val initialState = AugmentedState(
            intention = intention,
            workspaceRoot = world.workspaceRoot.absolutePath
        )

        world.resultState = world.orchestrator.execute(initialState)
        assertNotNull(world.resultState, "Result state should never be null")
    }

    @When("I plan a feature with intention {string}")
    fun `plan a feature`(intention: String) {
        world.intention = intention

        val initialState = AugmentedState(
            intention = intention,
            workspaceRoot = world.workspaceRoot.absolutePath
        )

        world.resultState = world.orchestrator.planFeature(initialState)
        assertNotNull(world.resultState, "Result state should never be null")
    }

    @When("I plan an architecture with intention {string}")
    fun `plan an architecture`(intention: String) {
        world.intention = intention

        val initialState = AugmentedState(
            intention = intention,
            workspaceRoot = world.workspaceRoot.absolutePath
        )

        world.resultState = world.orchestrator.planArchitecture(initialState)
        assertNotNull(world.resultState, "Result state should never be null")
    }

    @When("I plan a refactor with intention {string}")
    fun `plan a refactor`(intention: String) {
        world.intention = intention

        val initialState = AugmentedState(
            intention = intention,
            workspaceRoot = world.workspaceRoot.absolutePath
        )

        world.resultState = world.orchestrator.planRefactor(initialState)
        assertNotNull(world.resultState, "Result state should never be null")
    }

    @When("I plan documentation with intention {string}")
    fun `plan documentation`(intention: String) {
        world.intention = intention

        val initialState = AugmentedState(
            intention = intention,
            workspaceRoot = world.workspaceRoot.absolutePath
        )

        world.resultState = world.orchestrator.planDocumentation(initialState)
        assertNotNull(world.resultState, "Result state should never be null")
    }

    // ── Then ──

    @Then("the planning result state is not null")
    fun `planning result state is not null`() {
        assertNotNull(world.resultState, "Result state should be non-null after execution")
    }

    @Then("the planning classification is {string}")
    fun `planning classification is`(expected: String) {
        val classification = world.resultState?.classification
        assertNotNull(world.resultState, "Result state should exist")
        assertTrue(
            classification == expected,
            "Expected classification '$expected', got '$classification'"
        )
    }

    @Then("the intention is prefixed with {string}")
    fun `intention is prefixed with`(prefix: String) {
        val intention = world.resultState?.intention
        assertNotNull(world.resultState, "Result state should exist")
        assertTrue(
            intention?.startsWith(prefix) == true,
            "Intention should start with '$prefix', got '$intention'"
        )
    }

    @Then("a planning Mermaid diagram is generated")
    fun `planning mermaid diagram is generated`() {
        val diagram = world.orchestrator.asMermaidDiagram()
        assertNotNull(diagram, "Mermaid diagram should not be null")
        assertTrue(diagram.contains("augmented-planning"), "Diagram should contain strategy name")
        assertTrue(diagram.contains("buildContext"), "Diagram should contain buildContext node")
        assertTrue(diagram.contains("classify"), "Diagram should contain classify node")
        assertTrue(diagram.contains("plan"), "Diagram should contain plan node")
    }

    @Then("the planning intention is preserved in the result state")
    fun `planning intention is preserved in result state`() {
        assertNotNull(world.resultState, "Result state should exist")
        assertTrue(
            world.resultState!!.intention.contains(world.intention),
            "Intention should be preserved — expected to contain '${world.intention}', got '${world.resultState!!.intention}'"
        )
    }
}