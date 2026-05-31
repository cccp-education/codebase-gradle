package codebase.koog

import vibecoding.contracts.state.AugmentedState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KoogPlanningOrchestratorTest {

    @Test
    fun `orchestrator can be instantiated`() {
        val orchestrator = KoogPlanningOrchestrator()
        assertNotNull(orchestrator, "Orchestrator should be instantiable")
    }

    @Test
    fun `execute returns result state without pgvector`(@TempDir tempDir: File) {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "Add unit tests",
            workspaceRoot = tempDir.absolutePath
        )

        val result = orchestrator.execute(initialState)

        assertNotNull(result, "Result should not be null")
        assertEquals("Add unit tests", result.intention, "Intention should be preserved")
        assertTrue(
            result.error?.contains("BuildContextFailed") == true ||
            result.planError?.contains("CompositeContext unavailable") == true,
            "Should indicate context building failed without pgvector"
        )
    }

    @Test
    fun `planFeature adds FEATURE prefix`() {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "dark mode toggle",
            workspaceRoot = "/tmp"
        )

        val result = orchestrator.planFeature(initialState)

        assertNotNull(result, "Result should not be null")
        assertEquals("FEATURE: dark mode toggle", result.intention, "Should prefix with FEATURE")
    }

    @Test
    fun `planArchitecture adds ARCHITECTURE prefix`() {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "microservices design",
            workspaceRoot = "/tmp"
        )

        val result = orchestrator.planArchitecture(initialState)

        assertNotNull(result, "Result should not be null")
        assertEquals("ARCHITECTURE: microservices design", result.intention, "Should prefix with ARCHITECTURE")
    }

    @Test
    fun `planRefactor adds REFACTOR prefix`() {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "extract duplicate code",
            workspaceRoot = "/tmp"
        )

        val result = orchestrator.planRefactor(initialState)

        assertNotNull(result, "Result should not be null")
        assertEquals("REFACTOR: extract duplicate code", result.intention, "Should prefix with REFACTOR")
    }

    @Test
    fun `planDocumentation adds DOCUMENTATION prefix`() {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "update API docs",
            workspaceRoot = "/tmp"
        )

        val result = orchestrator.planDocumentation(initialState)

        assertNotNull(result, "Result should not be null")
        assertEquals("DOCUMENTATION: update API docs", result.intention, "Should prefix with DOCUMENTATION")
    }

    @Test
    fun `asMermaidDiagram generates diagram`() {
        val orchestrator = KoogPlanningOrchestrator()
        val diagram = orchestrator.asMermaidDiagram()

        assertNotNull(diagram, "Diagram should not be null")
        assertTrue(diagram.contains("augmented-planning"), "Should contain strategy name")
        assertTrue(diagram.contains("buildContext"), "Should contain buildContext node")
        assertTrue(diagram.contains("classify"), "Should contain classify node")
        assertTrue(diagram.contains("plan"), "Should contain plan node")
    }

    @Test
    fun `complex intention gets complex classification`(@TempDir tempDir: File) {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "Refactor cross-borough DAG N1→N2→N3 pour intégration multi-plugins avec architecture distribuée",
            workspaceRoot = tempDir.absolutePath
        )

        // Simuler un contexte valide pour que la classification fonctionne
        val stateWithContext = initialState.copy(
            compositeContext = contracts.context.CompositeContext(
                eagerSection = "rules",
                ragSection = "docs",
                graphifySection = "relations",
                docsSection = "manuals",
                config = contracts.context.CompositeContextConfig()
            )
        )

        val result = orchestrator.execute(stateWithContext)

        assertEquals("complexe", result.classification, "Long intention should be classified as complex")
    }

    @Test
    fun `simple intention gets simple classification`(@TempDir tempDir: File) {
        val orchestrator = KoogPlanningOrchestrator()
        val initialState = AugmentedState(
            intention = "Add dark mode toggle",
            workspaceRoot = tempDir.absolutePath
        )

        // Simuler un contexte valide
        val stateWithContext = initialState.copy(
            compositeContext = contracts.context.CompositeContext(
                eagerSection = "rules",
                ragSection = "docs",
                graphifySection = "relations",
                docsSection = "manuals",
                config = contracts.context.CompositeContextConfig()
            )
        )

        val result = orchestrator.execute(stateWithContext)

        assertEquals("simple", result.classification, "Short intention should be classified as simple")
    }
}