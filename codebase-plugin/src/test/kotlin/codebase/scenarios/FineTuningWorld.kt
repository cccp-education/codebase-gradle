package codebase.scenarios

import codebase.finetuning.FakeFineTuner
import codebase.finetuning.FineTuningGraph
import codebase.finetuning.FineTuningPipeline
import codebase.finetuning.FineTuningResult
import codebase.finetuning.FineTuningState
import codebase.finetuning.OllamaFineTunerAdapter
import codebase.finetuning.OllamaRegistryClient
import codebase.finetuning.RegistryResponse
import codebase.finetuning.CreateModelRequest
import codebase.finetuning.PushModelRequest
import java.nio.file.Files

/**
 * Shared world for `@finetuning` scenarios (EPIC FT-PIPELINE US-5).
 *
 * Holds the mutable state flowing between Given/When/Then steps:
 *  - the [pipeline] under test (either [FakeFineTuner] for graph scenarios
 *    or [OllamaFineTunerAdapter] wired to a stub [OllamaRegistryClient]
 *    for pipeline/fallback scenarios);
 *  - the [registryStub] recording create/push responses to assert degraded
 *    behavior;
 *  - the last [result] and [finalState] produced by the scenario.
 *
 * Pattern `VibeHardening2World` / `SubgraphWorld` (PicoContainer-scoped,
 * one fresh instance per scenario via the `@Given` init step).
 */
class FineTuningWorld {

    var registryStub: StubRegistryClient? = null
    var pipeline: FineTuningPipeline? = null
    var fakeFineTuner: FakeFineTuner? = null
    var finetuningGraph: FineTuningGraph? = null
    var result: FineTuningResult? = null
    var finalState: FineTuningState? = null

    fun ensureInitialized() {
        // PicoContainer instantiates the world; nothing else to bootstrap.
    }
}

/**
 * In-memory [OllamaRegistryClient] stub — records the responses to return
 * for `createModel` / `pushModel` and captures the last requests for
 * assertions. No network, no I/O (pattern `FakeFineTuner`).
 */
class StubRegistryClient(
    private val createResponse: RegistryResponse = RegistryResponse.ok(),
    private val pushResponse: RegistryResponse = RegistryResponse.ok(),
) : OllamaRegistryClient {

    var lastCreateRequest: CreateModelRequest? = null
        private set
    var lastPushRequest: PushModelRequest? = null
        private set

    override fun createModel(request: CreateModelRequest): RegistryResponse {
        lastCreateRequest = request
        return createResponse
    }

    override fun pushModel(request: PushModelRequest): RegistryResponse {
        lastPushRequest = request
        return pushResponse
    }
}