package codebase.finetuning

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

/**
 * EPIC FT-PIPELINE US-2 — `OllamaFineTunerAdapter` tests.
 *
 * L'adapter ponte le port [FineTuningPipeline] vers l'API Ollama
 * (`/api/create` create model from Modelfile + `/api/push` to registry).
 *
 * Testabilité : un [OllamaRegistryClient] (port interne fun interface)
 * est injecté via constructeur. En production, [OllamaHttpRegistryClient]
 * (`HttpClient.newHttpClient()`). En test, une lambda déterministe.
 *
 * Fallback degraded (pattern `AudioPostProcessor.process`) : Ollama
 * unavailable (HTTP 5xx, IOException) → `FineTuningResult.Failure`
 * avec dataset original préservé — pas de crash.
 */
class OllamaFineTunerAdapterTest {

    @TempDir
    lateinit var tmpDir: Path

    private val request = FineTuningRequest(
        baseModel = "gpt-oss:120b-cloud",
        dataset = listOf("docs/afnor/**/*.adoc"),
        outputModelName = "expert-cda",
        corpusRatio = 0.10
    )

    @Test
    fun `fineTune returns Success when Ollama create and push succeed`() {
        val client = StubOllamaRegistryClient(
            createResponse = RegistryResponse.ok(),
            pushResponse = RegistryResponse.ok()
        )
        val adapter = OllamaFineTunerAdapter(
            registryClient = client,
            ggufOutputDir = tmpDir
        )

        val result = adapter.fineTune(request)

        assertTrue(result is FineTuningResult.Success)
        val success = result as FineTuningResult.Success
        assertTrue(success.outputModelName == "expert-cda")
        assertTrue(success.ggufPath.endsWith("expert-cda.gguf"))
        assertTrue(success.validationScore in 0.0..1.0)
        assertTrue(client.createCalled)
        assertTrue(client.pushCalled)
    }

    @Test
    fun `fineTune returns Failure when Ollama create fails HTTP 500`() {
        val client = StubOllamaRegistryClient(
            createResponse = RegistryResponse.fail(500, "internal error"),
            pushResponse = RegistryResponse.ok()
        )
        val adapter = OllamaFineTunerAdapter(
            registryClient = client,
            ggufOutputDir = tmpDir
        )

        val result = adapter.fineTune(request)

        assertTrue(result is FineTuningResult.Failure)
        val failure = result as FineTuningResult.Failure
        assertTrue(failure.reason.contains("create"))
        assertTrue(failure.originalDataset == listOf("docs/afnor/**/*.adoc"))
        assertTrue(!client.pushCalled)
    }

    @Test
    fun `fineTune returns Failure when Ollama push fails HTTP 404`() {
        val client = StubOllamaRegistryClient(
            createResponse = RegistryResponse.ok(),
            pushResponse = RegistryResponse.fail(404, "registry not found")
        )
        val adapter = OllamaFineTunerAdapter(
            registryClient = client,
            ggufOutputDir = tmpDir
        )

        val result = adapter.fineTune(request)

        assertTrue(result is FineTuningResult.Failure)
        val failure = result as FineTuningResult.Failure
        assertTrue(failure.reason.contains("push"))
        assertTrue(failure.originalDataset == listOf("docs/afnor/**/*.adoc"))
        assertTrue(client.createCalled)
    }

    @Test
    fun `fineTune returns Failure when registryClient throws`() {
        val client = StubOllamaRegistryClient(throwOnCall = true)
        val adapter = OllamaFineTunerAdapter(
            registryClient = client,
            ggufOutputDir = tmpDir
        )

        val result = adapter.fineTune(request)

        assertTrue(result is FineTuningResult.Failure)
        val failure = result as FineTuningResult.Failure
        assertTrue(failure.originalDataset == listOf("docs/afnor/**/*.adoc"))
    }

    @Test
    fun `adapter implements FineTuningPipeline`() {
        val client = StubOllamaRegistryClient()
        val adapter = OllamaFineTunerAdapter(
            registryClient = client,
            ggufOutputDir = tmpDir
        )
        assertTrue(adapter is FineTuningPipeline)
    }

    private class StubOllamaRegistryClient(
        private val createResponse: RegistryResponse = RegistryResponse.ok(),
        private val pushResponse: RegistryResponse = RegistryResponse.ok(),
        private val throwOnCall: Boolean = false
    ) : OllamaRegistryClient {
        var createCalled: Boolean = false
            private set
        var pushCalled: Boolean = false
            private set

        override fun createModel(request: CreateModelRequest): RegistryResponse {
            createCalled = true
            if (throwOnCall) throw java.io.IOException("connection refused (stub)")
            return createResponse
        }

        override fun pushModel(request: PushModelRequest): RegistryResponse {
            pushCalled = true
            if (throwOnCall) throw java.io.IOException("connection refused (stub)")
            return pushResponse
        }
    }
}