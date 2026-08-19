package codebase.scenarios

import codebase.finetuning.FakeFineTuner
import codebase.finetuning.FineTuningConfig
import codebase.finetuning.FineTuningGraph
import codebase.finetuning.FineTuningLlm
import codebase.finetuning.FineTuningPromptBuilder
import codebase.finetuning.FineTuningRequest
import codebase.finetuning.FineTuningResult
import codebase.finetuning.FineTuningStage
import codebase.finetuning.FineTuningState
import codebase.finetuning.OllamaFineTunerAdapter
import codebase.finetuning.RegistryResponse
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@finetuning` scenarios (EPIC FT-PIPELINE US-5).
 *
 * Steps are prefixed "finetuning" to avoid glue collisions with other
 * feature step classes sharing the `codebase.scenarios` package
 * (pattern S-088 — VibeHardening2Steps, SubgraphSteps).
 *
 * Pure BDD: no Gradle task is invoked, no network call is issued — the
 * scenarios drive the domain `codebase.finetuning` via fakes/stubs
 * ([FakeFineTuner], [StubRegistryClient], [FakeFineTuningLlm],
 * [StubPromptBuilder]).
 */
class FineTuningSteps(private val world: FineTuningWorld) {

    @Given("a finetuning world is initialized")
    fun `finetuning world initialized`() {
        world.ensureInitialized()
        assertNotNull(world, "FineTuningWorld should be instantiated by PicoContainer")
    }

    @Given("a finetuning pipeline wired to an Ollama registry returning success")
    fun `pipeline wired to success registry`() {
        val stub = StubRegistryClient(
            createResponse = RegistryResponse.ok(),
            pushResponse = RegistryResponse.ok(),
        )
        world.registryStub = stub
        world.pipeline = OllamaFineTunerAdapter(
            registryClient = stub,
            ggufOutputDir = Files.createTempDirectory("finetuning-cucumber").toFile().toPath(),
        )
    }

    @Given("a finetuning pipeline wired to an Ollama registry returning a 503 failure")
    fun `pipeline wired to 503 registry`() {
        val stub = StubRegistryClient(
            createResponse = RegistryResponse.fail(503, "service unavailable"),
            pushResponse = RegistryResponse.ok(),
        )
        world.registryStub = stub
        world.pipeline = OllamaFineTunerAdapter(
            registryClient = stub,
            ggufOutputDir = Files.createTempDirectory("finetuning-cucumber").toFile().toPath(),
        )
    }

    @Given("a finetuning graph with a fake LLM proposing ratio {double}, validating {double} then {double}, and a fake pipeline always succeeding")
    fun `graph with loop llm`(proposeRatio: Double, firstScore: Double, secondScore: Double) {
        val llm = FakeFineTuningLlm(
            proposeResponses = listOf("corpus ratio $proposeRatio", "corpus ratio $proposeRatio"),
            validateResponses = listOf(firstScore, secondScore),
        )
        val pipeline = FakeFineTuner(defaultResult = fineTuningSuccessStub())
        world.fakeFineTuner = pipeline
        world.finalState = null
        world.registryStub = null
        world.pipeline = null
        // Stash the graph pieces on the world via a transient holder.
        world.finetuningGraph = FineTuningGraph(
            promptBuilder = StubPromptBuilder,
            llm = llm,
            pipeline = pipeline,
        )
    }

    @Given("a finetuning graph with a fake LLM proposing ratio {double}, validating {double}, and a fake pipeline always succeeding")
    fun `graph with single pass llm`(proposeRatio: Double, score: Double) {
        val llm = FakeFineTuningLlm(
            proposeResponses = listOf("corpus ratio $proposeRatio"),
            validateResponses = listOf(score),
        )
        val pipeline = FakeFineTuner(defaultResult = fineTuningSuccessStub())
        world.fakeFineTuner = pipeline
        world.finalState = null
        world.registryStub = null
        world.pipeline = null
        world.finetuningGraph = FineTuningGraph(
            promptBuilder = StubPromptBuilder,
            llm = llm,
            pipeline = pipeline,
        )
    }

    @When("the finetuning pipeline fine-tunes a request with base model {string}, dataset {string} and output model {string}")
    fun `pipeline fine tunes`(baseModel: String, dataset: String, outputModel: String) {
        val request = FineTuningRequest(
            baseModel = baseModel,
            dataset = listOf(dataset),
            outputModelName = outputModel,
        )
        world.result = world.pipeline!!.fineTune(request)
    }

    @When("the finetuning graph executes from an initial state with threshold {double} and max {int} iterations")
    fun `graph executes`(threshold: Double, maxIterations: Int) {
        val request = FineTuningRequest(
            baseModel = "gpt-oss:120b-cloud",
            dataset = listOf("docs/afnor/**/*.adoc"),
            outputModelName = "expert-cda",
        )
        val initial = FineTuningState(
            request = request,
            config = FineTuningConfig(),
            validationThreshold = threshold,
            maxIterations = maxIterations,
        )
        world.finalState = world.finetuningGraph!!.execute(initial)
    }

    @Then("the finetuning pipeline returns a success result")
    fun `pipeline returns success`() {
        val result = world.result
        assertNotNull(result, "Fine-tuning should return a result")
        assertTrue(result is FineTuningResult.Success, "Expected Success, got $result")
    }

    @Then("the finetuning pipeline returns a failure result")
    fun `pipeline returns failure`() {
        val result = world.result
        assertNotNull(result, "Fine-tuning should return a result")
        assertTrue(result is FineTuningResult.Failure, "Expected Failure, got $result")
    }

    @And("the finetuning pipeline success result references output model {string}")
    fun `success references output model`(expected: String) {
        val result = world.result as FineTuningResult.Success
        assertEquals(expected, result.outputModelName)
    }

    @And("the finetuning pipeline success result references a non-blank GGUF path")
    fun `success references gguf path`() {
        val result = world.result as FineTuningResult.Success
        assertTrue(result.ggufPath.isNotBlank(), "GGUF path should be non-blank, got '${result.ggufPath}'")
    }

    @And("the finetuning pipeline success result reports {int} iteration and a validation score of {double}")
    fun `success reports iterations and score`(iterations: Int, score: Double) {
        val result = world.result as FineTuningResult.Success
        assertEquals(iterations, result.iterations)
        assertEquals(score, result.validationScore, 1e-9)
    }

    @And("the finetuning pipeline failure reason mentions the registry failure")
    fun `failure mentions registry`() {
        val result = world.result as FineTuningResult.Failure
        assertTrue(
            result.reason.contains("503") || result.reason.contains("create failed"),
            "Failure reason should mention the registry failure, got: ${result.reason}"
        )
    }

    @And("the finetuning pipeline failure preserves the original dataset {string}")
    fun `failure preserves dataset`(dataset: String) {
        val result = world.result as FineTuningResult.Failure
        assertEquals(listOf(dataset), result.originalDataset)
    }

    @Then("the finetuning graph final stage is CONVERGED")
    fun `graph final stage converged`() {
        val state = world.finalState
        assertNotNull(state, "Graph should produce a final state")
        assertEquals(FineTuningStage.CONVERGED, state.stage, "Expected CONVERGED, got ${state.stage} (error=${state.error})")
    }

    @And("the finetuning graph final validation score is {double}")
    fun `graph final score`(expected: Double) {
        val state = world.finalState!!
        assertEquals(expected, state.validationScore, 1e-9)
    }

    @And("the finetuning graph final iteration is {int}")
    fun `graph final iteration`(expected: Int) {
        val state = world.finalState!!
        assertEquals(expected, state.iteration)
    }

    private fun fineTuningSuccessStub(): FineTuningResult =
        FineTuningResult.success(
            outputModelName = "expert-cda",
            ggufPath = "/tmp/finetuning-cucumber/expert-cda.gguf",
            iterations = 1,
            validationScore = 1.0,
        )
}

/**
 * Fake [FineTuningLlm] for `@finetuning` graph scenarios — FIFO queues for
 * [propose] and [validate] responses (pattern `FakeFineTuner`).
 *
 * The iterative graph calls `propose` once per iteration, so callers must
 * size [proposeResponses] accordingly.
 */
class FakeFineTuningLlm(
    private val proposeResponses: List<String>,
    private val validateResponses: List<Double>,
) : FineTuningLlm {

    private val proposeQueue = proposeResponses.toMutableList()
    private val validateQueue = validateResponses.toMutableList()

    override fun propose(prompt: String): String {
        return proposeQueue.removeAt(0)
    }

    override fun validate(prompt: String): Double {
        return validateQueue.removeAt(0)
    }
}

/**
 * Stub [FineTuningPromptBuilder] — deterministic non-blank prompts, no
 * LLM, no I/O. The graph only requires non-blank prompts to flow through
 * the nodes.
 */
object StubPromptBuilder : FineTuningPromptBuilder {
    override fun buildProposePrompt(state: FineTuningState): String =
        "propose corpus ratio adjustment for ${state.request.outputModelName} (iteration ${state.iteration})"

    override fun buildValidatePrompt(state: FineTuningState): String =
        "validate fine-tuned model ${state.request.outputModelName}"
}