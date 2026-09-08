package codebase.store

import kotlinx.serialization.Serializable

/**
 * Doubt metadata transported at RAG ingestion time (EPIC OCR-QUALITY US-4).
 *
 * Derived from the OCR engine confidence (codex N2 produces the raw
 * measurement, codebase N1 transports it) — attached to the ingestion,
 * never to the embeddings, so no re-vectorization is needed
 * (Loi de l'Économie d'Encre).
 *
 * @property confidence OCR confidence score in [0.0, 1.0]
 * @property doubtful explicit doubt flag (wins over the threshold heuristic)
 */
@Serializable
data class DoubtMetadata(
    val confidence: Double = MAX_CONFIDENCE,
    val doubtful: Boolean = confidence < DEFAULT_THRESHOLD
) {
    init {
        require(confidence in 0.0..1.0) { "confidence must be in [0.0, 1.0], was $confidence" }
    }

    companion object {
        /** Default doubt threshold — chunks under this confidence are flagged. */
        const val DEFAULT_THRESHOLD = 0.5

        /** Max confidence value — a confident chunk carries full confidence. */
        const val MAX_CONFIDENCE = 1.0

        /** Derives a [DoubtMetadata] from a raw OCR confidence score. */
        fun fromConfidence(confidence: Double, threshold: Double = DEFAULT_THRESHOLD): DoubtMetadata =
            DoubtMetadata(confidence = confidence, doubtful = confidence < threshold)
    }
}