package codebase.ocr

import codebase.koog.llm.ThrowingVisionProvider
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * Boundary tests (EPIC CDX-OCR-BOUNDARY US-2): the codebase OCR task is
 * AI-only. Software OCR (Tesseract) is actioned by codex via `collectOcr`.
 */
class OcrTaskAiOnlyBoundaryTest {

    @Test
    fun `tesseract provider is rejected with boundary message`(@TempDir dir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(dir.toFile()).withName("ocr-tess").build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val imgFile = dir.resolve("tess.png").toFile()
        imgFile.writeBytes(createMinimalPng())

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set("tesseract")
        task.inputFile.set(project.layout.projectDirectory.file("tess.png"))
        task.ocrLanguage.set("eng")
        task.outputFormat.set("asciidoc")

        val exception = assertThrows<GradleException> { task.executeOcr() }
        assertTrue(
            exception.message!!.contains("codex"),
            "Error message should point to the codex plugin: ${exception.message}"
        )
    }

    @Test
    fun `gemini+ollama raises error when both providers fail without software fallback`(@TempDir dir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(dir.toFile()).withName("ocr-tess-fb").build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val imgFile = dir.resolve("fallback.png").toFile()
        imgFile.writeBytes(createMinimalPng())

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set("gemini+ollama")
        task.inputFile.set(project.layout.projectDirectory.file("fallback.png"))
        task.ocrLanguage.set("eng")
        task.outputFormat.set("asciidoc")
        task.geminiVisionProvider = ThrowingVisionProvider()
        task.ollamaOcrProvider = ThrowingVisionProvider()

        val exception = assertThrows<GradleException> { task.executeOcr() }
        assertTrue(
            exception.message == OcrTask.AI_ONLY_ERROR_MESSAGE,
            "Error should be the AI-only boundary message: ${exception.message}"
        )
    }

    private fun createMinimalPng(): ByteArray {
        val pngHex = "89504E470D0A1A0A0000000D4948445200000001000000010802000000907" +
            "71DE0000000C4944415408D763F8FFFF3F000005005E018246A4B10000000049" +
            "454E44AE426082"
        val cleaned = pngHex.replace(Regex("[^0-9A-Fa-f]"), "")
        val bytes = ByteArray(cleaned.length / 2)
        for (i in bytes.indices) {
            bytes[i] = (cleaned.substring(i * 2, i * 2 + 2).toInt(16) and 0xFF).toByte()
        }
        return bytes
    }
}
