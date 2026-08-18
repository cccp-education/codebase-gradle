package codebase.finetuning

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * EPIC FT-PIPELINE US-3 — `FineTuningState` + `FineTuningStage` tests.
 *
 * `FineTuningState` is an immutable koog state (pattern `slider.pipeline.DeckState`).
 * 11 fields model the full iterative lifecycle — propose/train/validate loop,
 * backtracking on insufficient validation score, convergence when score >= threshold,
 * failure when the iteration budget is exhausted.
 *
 * Invariants covered: validationScore/threshold in `[0,1]`, iteration in
 * `[0, maxIterations]`, cross-field consistency (stage vs proposal/trainResult/
 * error), stage-specific requires.
 */
class FineTuningStateTest {

    private val request = FineTuningRequest(
        baseModel = "gpt-oss:120b-cloud",
        dataset = listOf("docs/afnor/**/*.adoc"),
        outputModelName = "expert-cda",
    )

    private val success = FineTuningResult.Success(
        outputModelName = "expert-cda",
        ggufPath = "/tmp/expert-cda.gguf",
        iterations = 1,
        validationScore = 0.82,
    )

    @Test
    fun `default state is INITIALIZED with default config and zero iterations`() {
        val state = FineTuningState(request = request)

        assertEquals(FineTuningStage.INITIALIZED, state.stage)
        assertEquals(0, state.iteration)
        assertEquals(3, state.maxIterations)
        assertEquals(0.7, state.validationThreshold, 1e-9)
        assertEquals(0.0, state.validationScore, 1e-9)
        assertEquals("", state.proposal)
        assertNull(state.trainResult)
        assertNull(state.error)
        assertEquals(FineTuningConfig(), state.config)
    }

    @Test
    fun `state can be created at PROPOSED stage with non-blank proposal`() {
        val state = FineTuningState(
            request = request,
            proposal = "increase corpus ratio to 0.15",
            stage = FineTuningStage.PROPOSED,
        )

        assertEquals(FineTuningStage.PROPOSED, state.stage)
        assertTrue(state.proposal.isNotBlank())
    }

    @Test
    fun `PROPOSED stage rejects blank proposal`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                proposal = "",
                stage = FineTuningStage.PROPOSED,
            )
        }
        assertTrue(ex.message!!.contains("proposal"))
    }

    @Test
    fun `proposal non-blank requires stage at least PROPOSED`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                proposal = "proposal before PROPOSED stage",
                stage = FineTuningStage.INITIALIZED,
            )
        }
        assertTrue(ex.message!!.contains("proposal"))
    }

    @Test
    fun `TRAINED stage requires trainResult non-null`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                trainResult = null,
                stage = FineTuningStage.TRAINED,
            )
        }
        assertTrue(ex.message!!.contains("trainResult"))
    }

    @Test
    fun `trainResult non-null requires stage at least TRAINED`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                trainResult = success,
                stage = FineTuningStage.INITIALIZED,
            )
        }
        assertTrue(ex.message!!.contains("trainResult"))
    }

    @Test
    fun `CONVERGED stage requires validationScore above threshold`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                trainResult = success,
                validationScore = 0.4,
                validationThreshold = 0.7,
                stage = FineTuningStage.CONVERGED,
            )
        }
        assertTrue(ex.message!!.contains("validationScore"))
    }

    @Test
    fun `CONVERGED stage requires trainResult Success`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                trainResult = FineTuningResult.Failure("boom", request.dataset),
                validationScore = 0.9,
                validationThreshold = 0.7,
                stage = FineTuningStage.CONVERGED,
            )
        }
        assertTrue(ex.message!!.contains("trainResult"))
    }

    @Test
    fun `CONVERGED stage accepts a valid converged state`() {
        val state = FineTuningState(
            request = request,
            trainResult = success,
            validationScore = 0.85,
            validationThreshold = 0.7,
            stage = FineTuningStage.CONVERGED,
        )
        assertEquals(FineTuningStage.CONVERGED, state.stage)
        assertTrue(state.validationScore >= state.validationThreshold)
    }

    @Test
    fun `FAILED stage requires non-blank error`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                error = "",
                stage = FineTuningStage.FAILED,
            )
        }
        assertTrue(ex.message!!.contains("error"))
    }

    @Test
    fun `error non-null requires stage FAILED`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                error = "should not be here",
                stage = FineTuningStage.INITIALIZED,
            )
        }
        assertTrue(ex.message!!.contains("error"))
    }

    @Test
    fun `FAILED stage accepts a valid failed state with error`() {
        val state = FineTuningState(
            request = request,
            error = "maxIterations exhausted",
            stage = FineTuningStage.FAILED,
        )
        assertEquals(FineTuningStage.FAILED, state.stage)
        assertEquals("maxIterations exhausted", state.error)
    }

    @Test
    fun `validationScore must be in 0 to 1 range`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(request = request, validationScore = 1.5)
        }
        assertTrue(ex.message!!.contains("validationScore"))
    }

    @Test
    fun `validationThreshold must be in 0 to 1 range`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(request = request, validationThreshold = -0.1)
        }
        assertTrue(ex.message!!.contains("validationThreshold"))
    }

    @Test
    fun `maxIterations must be positive`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(request = request, maxIterations = 0)
        }
        assertTrue(ex.message!!.contains("maxIterations"))
    }

    @Test
    fun `iteration must be in 0 to maxIterations range`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(request = request, maxIterations = 3, iteration = 5)
        }
        assertTrue(ex.message!!.contains("iteration"))
    }

    @Test
    fun `VALIDATED stage requires trainResult non-null`() {
        val ex = assertThrows<IllegalArgumentException> {
            FineTuningState(
                request = request,
                trainResult = null,
                stage = FineTuningStage.VALIDATED,
            )
        }
        assertTrue(ex.message!!.contains("trainResult"))
    }

    @Test
    fun `copy preserves request and config through stage transitions`() {
        val config = FineTuningConfig(epochs = 5, learningRate = 1e-4)
        val state = FineTuningState(request = request, config = config)
        val proposed = state.copy(
            proposal = "use more data",
            stage = FineTuningStage.PROPOSED,
        )

        assertEquals(request, proposed.request)
        assertEquals(config, proposed.config)
        assertEquals(FineTuningStage.PROPOSED, proposed.stage)
    }
}