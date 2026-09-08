package codebase.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * EPIC OCR-QUALITY US-4 — doubt metadata DDD value object and additive
 * DDL templates (baby-step TDD strict).
 *
 * Doubt travels at ingestion time (not at embedding time): `confidence`
 * and the `doubtful` flag ride along with the chunk into pgvector
 * columns, so the augmented context can weight or exclude shaky
 * passages without any re-vectorization.
 */
class DoubtMetadataTest {

    @Test
    fun `DoubtMetadata holds confidence and doubtful flag`() {
        val doubt = DoubtMetadata(confidence = 0.35, doubtful = true)
        assertEquals(0.35, doubt.confidence)
        assertTrue(doubt.doubtful)
    }

    @Test
    fun `DoubtMetadata default is confident and not doubtful`() {
        val doubt = DoubtMetadata()
        assertEquals(DoubtMetadata.MAX_CONFIDENCE, doubt.confidence)
        assertFalse(doubt.doubtful)
    }

    @Test
    fun `confidence below 0 is rejected`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            DoubtMetadata(confidence = -0.1)
        }
    }

    @Test
    fun `confidence above 1 is rejected`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            DoubtMetadata(confidence = 1.1)
        }
    }

    @Test
    fun `confidence at boundaries is accepted`() {
        assertEquals(0.0, DoubtMetadata(confidence = 0.0).confidence)
        assertEquals(1.0, DoubtMetadata(confidence = 1.0).confidence)
    }

    @Test
    fun `confidence below threshold marks chunk doubtful`() {
        val doubt = DoubtMetadata(confidence = DoubtMetadata.DEFAULT_THRESHOLD - 0.01)
        assertTrue(doubt.doubtful)
    }

    @Test
    fun `confidence at or above threshold is not doubtful`() {
        val doubt = DoubtMetadata(confidence = DoubtMetadata.DEFAULT_THRESHOLD)
        assertFalse(doubt.doubtful)
    }

    @Test
    fun `explicit doubtful flag wins over threshold heuristic`() {
        val suspicious = DoubtMetadata(confidence = 0.9, doubtful = true)
        assertTrue(suspicious.doubtful)
        val flagged = DoubtMetadata(confidence = 0.1, doubtful = false)
        assertFalse(flagged.doubtful)
    }

    @Test
    fun `fromConfidence derives doubtful flag from default threshold`() {
        val doubt = DoubtMetadata.fromConfidence(0.2)
        assertTrue(doubt.confidence == 0.2)
        assertTrue(doubt.doubtful)
    }

    @Test
    fun `fromConfidence with custom threshold`() {
        val doubt = DoubtMetadata.fromConfidence(0.55, threshold = 0.5)
        assertFalse(doubt.doubtful)
        val low = DoubtMetadata.fromConfidence(0.45, threshold = 0.5)
        assertTrue(low.doubtful)
    }

    @Test
    fun `default threshold value is 0_5`() {
        assertEquals(0.5, DoubtMetadata.DEFAULT_THRESHOLD)
    }

    @Test
    fun `max confidence value is 1_0`() {
        assertEquals(1.0, DoubtMetadata.MAX_CONFIDENCE)
    }
}