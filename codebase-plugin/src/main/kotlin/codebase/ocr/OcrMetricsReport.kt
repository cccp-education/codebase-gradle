package codebase.ocr

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object OcrMetricsReport {

    fun generateAsciiDoc(metrics: List<OcrMetrics>): String {
        val date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val totalDuration = metrics.sumOf { it.ocrDurationMs + it.ingestDurationMs + it.embeddingDurationMs }
        val totalCost = metrics.sumOf { it.estimatedCostUsd }
        val totalInputTokens = metrics.sumOf { it.estimatedInputTokens }
        val totalOutputTokens = metrics.sumOf { it.estimatedOutputTokens }
        val totalFiles = metrics.size
        val totalChunks = metrics.sumOf { it.chunkCount }
        val totalReplacements = metrics.sumOf { it.anonymizationReplacements }

        val sb = StringBuilder()
        sb.appendLine("= Rapport Métriques OCR")
        sb.appendLine(":date: $date")
        sb.appendLine(":toc:")
        sb.appendLine()
        sb.appendLine("== Résumé")
        sb.appendLine()
        sb.appendLine("[cols=\"2,3\", options=\"header\"]")
        sb.appendLine("|===")
        sb.appendLine("| Métrique | Valeur")
        sb.appendLine("| Fichiers traités | $totalFiles")
        sb.appendLine("| Durée totale | ${OcrMetricsCalculator.formatDurationMs(totalDuration)}")
        sb.appendLine("| Coût total estimé (USD) | ${"%.6f".format(java.util.Locale.US, totalCost)}")
        sb.appendLine("| Tokens input estimés | $totalInputTokens")
        sb.appendLine("| Tokens output estimés | $totalOutputTokens")
        sb.appendLine("| Chunks générés | $totalChunks")
        sb.appendLine("| Remplacements anonymisation | $totalReplacements")
        sb.appendLine("|===")
        sb.appendLine()
        sb.appendLine("== Détail par Fichier")
        sb.appendLine()
        sb.appendLine("[cols=\"1,1,1,1,1,1,1\", options=\"header\"]")
        sb.appendLine("|===")
        sb.appendLine("| Fichier | Type | Fournisseur | Modèle | Durée OCR | Coût (USD) | Chunks")
        for (m in metrics) {
            val type = if (m.isImage) "image" else "texte"
            val duration = OcrMetricsCalculator.formatDurationMs(m.ocrDurationMs)
            val cost = "${"%.6f".format(java.util.Locale.US, m.estimatedCostUsd)}"
            sb.appendLine("| ${m.fileName} | $type | ${m.provider} | ${m.model} | $duration | $cost | ${m.chunkCount}")
        }
        sb.appendLine("|===")
        sb.appendLine()
        sb.appendLine("== Métriques Détaillées")
        sb.appendLine()
        for (m in metrics) {
            sb.appendLine("=== ${m.fileName}")
            sb.appendLine()
            sb.appendLine("[cols=\"2,3\"]")
            sb.appendLine("|===")
            sb.appendLine("| Propriété | Valeur")
            sb.appendLine("| Taille fichier | ${m.fileSizeBytes} bytes")
            sb.appendLine("| Type | ${if (m.isImage) "Image" else "Texte"}")
            sb.appendLine("| Langue | ${m.language}")
            sb.appendLine("| Durée OCR | ${OcrMetricsCalculator.formatDurationMs(m.ocrDurationMs)}")
            sb.appendLine("| Longueur sortie | ${m.outputLengthChars} caractères")
            sb.appendLine("| Tokens input (estimé) | ${m.estimatedInputTokens}")
            sb.appendLine("| Tokens output (estimé) | ${m.estimatedOutputTokens}")
            sb.appendLine("| Coût estimé (USD) | ${"%.6f".format(java.util.Locale.US, m.estimatedCostUsd)}")
            sb.appendLine("| Remplacements anonymisation | ${m.anonymizationReplacements}")
            if (m.anonymizationCategories.isNotEmpty()) {
                sb.appendLine("| Catégories détectées | ${m.anonymizationCategories.joinToString(", ")}")
            }
            if (m.chunkCount > 0) {
                sb.appendLine("| Chunks | ${m.chunkCount}")
                sb.appendLine("| Durée ingestion | ${OcrMetricsCalculator.formatDurationMs(m.ingestDurationMs)}")
                sb.appendLine("| Durée embedding | ${OcrMetricsCalculator.formatDurationMs(m.embeddingDurationMs)}")
            }
            sb.appendLine("|===")
            sb.appendLine()
        }
        return sb.toString()
    }

    private fun StringBuilder.appendLine(line: String) {
        append(line)
        append('\n')
    }
}
