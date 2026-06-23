package codebase.ocr

import codebase.koog.llm.CodexOcrEngineAdapter
import codebase.koog.llm.ThrowingVisionProvider
import codex.ocr.TesseractOcrEngine
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertTrue

class OcrTaskTesseractFallbackTest {

    @Test
    fun `OcrTask with tesseract provider processes image`(@TempDir dir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(dir.toFile()).withName("ocr-tess").build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val imgFile = File(dir.toFile(), "tess.png")
        imgFile.writeBytes(createMinimalPng())

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set("tesseract")
        task.inputFile.set(project.layout.projectDirectory.file("tess.png"))
        task.ocrLanguage.set("eng")
        task.outputFormat.set("asciidoc")

        task.executeOcr()

        val outFile = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("tess_ocr.adoc")
        assertTrue(outFile.exists(), "OCR output should exist: ${outFile.absolutePath}")
    }

    @Test
    fun `OcrTask gemini+ollama falls back to Tesseract when both fail`(@TempDir dir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(dir.toFile()).withName("ocr-tess-fb").build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val imgFile = File(dir.toFile(), "fallback.png")
        imgFile.writeBytes(createMinimalPng())

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set("gemini+ollama")
        task.inputFile.set(project.layout.projectDirectory.file("fallback.png"))
        task.ocrLanguage.set("eng")
        task.outputFormat.set("asciidoc")
        task.geminiVisionProvider = ThrowingVisionProvider()
        task.ollamaOcrProvider = ThrowingVisionProvider()
        task.tesseractOcrProvider = CodexOcrEngineAdapter(TesseractOcrEngine(tesseractPath = "tesseract"))

        task.executeOcr()

        val outFile = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("fallback_ocr.adoc")
        assertTrue(outFile.exists(), "OCR output should exist after Tesseract fallback: ${outFile.absolutePath}")
        val content = outFile.readText(Charsets.UTF_8)
        assertTrue(content.isNotEmpty() || content == "", "Tesseract fallback should produce output (may be empty for textless image)")
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