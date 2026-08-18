package codebase.finetuning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * EPIC FT-PIPELINE US-2 — domaine `codebase.finetuning`.
 *
 * `FineTuningRequest` + `FineTuningResult` sealed + `FineTuningPipeline`
 * port + `FakeFineTuner` (pattern `TranscriptLlmEnhancer` port/fake).
 *
 * Invariants : baseModel non-blank, dataset non-empty, outputModelName
 * non-blank, corpusRatio 0.0..1.0 default 0.10.
 */
class FineTuningPipelineTest {

    @Test
    fun `FineTuningRequest defaults corpusRatio to 0_10`() {
        val request = FineTuningRequest(
            baseModel = "gpt-oss:120b-cloud",
            dataset = listOf("docs/afnor/**/*.adoc"),
            outputModelName = "expert-cda"
        )
        assertEquals(0.10, request.corpusRatio, 1e-9)
    }

    @Test
    fun `FineTuningRequest rejects blank baseModel`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningRequest(
                baseModel = "  ",
                dataset = listOf("docs/**/*.adoc"),
                outputModelName = "expert-cda"
            )
        }
        assertTrue(ex.message!!.contains("baseModel"))
    }

    @Test
    fun `FineTuningRequest rejects empty dataset`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningRequest(
                baseModel = "gpt-oss:120b-cloud",
                dataset = emptyList(),
                outputModelName = "expert-cda"
            )
        }
        assertTrue(ex.message!!.contains("dataset"))
    }

    @Test
    fun `FineTuningRequest rejects blank outputModelName`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningRequest(
                baseModel = "gpt-oss:120b-cloud",
                dataset = listOf("docs/**/*.adoc"),
                outputModelName = ""
            )
        }
        assertTrue(ex.message!!.contains("outputModelName"))
    }

    @Test
    fun `FineTuningRequest rejects corpusRatio out of range`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningRequest(
                baseModel = "gpt-oss:120b-cloud",
                dataset = listOf("docs/**/*.adoc"),
                outputModelName = "expert-cda",
                corpusRatio = 1.5
            )
        }
        assertTrue(ex.message!!.contains("corpusRatio"))
    }

    @Test
    fun `FineTuningResult Success holds outputModelName and ggufPath`() {
        val result = FineTuningResult.Success(
            outputModelName = "expert-cda",
            ggufPath = "/tmp/expert-cda.gguf",
            iterations = 3,
            validationScore = 0.82
        )
        assertEquals("expert-cda", result.outputModelName)
        assertEquals("/tmp/expert-cda.gguf", result.ggufPath)
        assertEquals(3, result.iterations)
        assertEquals(0.82, result.validationScore, 1e-9)
        assertTrue(result.isSuccess)
        assertTrue(!result.isFailure)
    }

    @Test
    fun `FineTuningResult Failure holds reason and preserves original dataset`() {
        val result = FineTuningResult.Failure(
            reason = "Ollama unavailable",
            originalDataset = listOf("docs/**/*.adoc")
        )
        assertEquals("Ollama unavailable", result.reason)
        assertEquals(listOf("docs/**/*.adoc"), result.originalDataset)
        assertTrue(result.isFailure)
        assertTrue(!result.isSuccess)
    }

    @Test
    fun `FakeFineTuner returns Success for valid request`() {
        val fake = FakeFineTuner(
            FineTuningResult.Success(
                outputModelName = "expert-cda",
                ggufPath = "/tmp/expert-cda.gguf",
                iterations = 1,
                validationScore = 0.9
            )
        )
        val request = FineTuningRequest(
            baseModel = "gpt-oss:120b-cloud",
            dataset = listOf("docs/**/*.adoc"),
            outputModelName = "expert-cda"
        )
        val result = fake.fineTune(request)
        assertTrue(result is FineTuningResult.Success)
        assertEquals("expert-cda", (result as FineTuningResult.Success).outputModelName)
    }

    @Test
    fun `FakeFineTuner captures last request for unit-test assertions`() {
        val fake = FakeFineTuner(FineTuningResult.Failure("no-op", emptyList()))
        val request = FineTuningRequest(
            baseModel = "gemma4:31b-cloud",
            dataset = listOf("docs/reac/**/*.adoc"),
            outputModelName = "expert-fpa"
        )
        fake.fineTune(request)
        assertEquals(request, fake.lastRequest)
        assertEquals(1, fake.callCount)
    }

    @Test
    fun `FakeFineTuner throws when no result configured`() {
        val fake = FakeFineTuner()
        val request = FineTuningRequest(
            baseModel = "gpt-oss:120b-cloud",
            dataset = listOf("docs/**/*.adoc"),
            outputModelName = "expert-cda"
        )
        assertThrows<IllegalStateException> { fake.fineTune(request) }
    }

    @Test
    fun `FakeFineTuner enqueueResult serves FIFO`() {
        val fake = FakeFineTuner()
        val first = FineTuningResult.Success("a", "/tmp/a.gguf", 1, 0.5)
        val second = FineTuningResult.Failure("boom", listOf("docs/**/*.adoc"))
        fake.enqueueResult(first)
        fake.enqueueResult(second)
        val request = FineTuningRequest(
            baseModel = "gpt-oss:120b-cloud",
            dataset = listOf("docs/**/*.adoc"),
            outputModelName = "expert-cda"
        )
        assertEquals(first, fake.fineTune(request))
        assertEquals(second, fake.fineTune(request))
        assertEquals(2, fake.callCount)
    }
}