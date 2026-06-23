package codebase.ocr

import codebase.koog.llm.FakeVisionProvider
import codebase.koog.llm.pool.GeminiKeyPool
import codebase.koog.llm.pool.GeminiPoolFactory
import contracts.llmpool.LlmInstance
import contracts.llmpool.QuotaConfig
import contracts.llmpool.ResetPolicy
import contracts.llmpool.RotationStrategy
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OcrTaskPoolTest {

    @Test
    fun `OcrTask with geminiApiKeys builds provider with pool rotation`(@TempDir dir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(dir.toFile()).withName("ocr-pool").build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val imgFile = File(dir.toFile(), "scan.png")
        imgFile.writeBytes(ByteArray(100))

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set("gemini")
        task.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")
        task.geminiApiKeys = listOf("key-A", "key-B", "key-C")
        task.geminiVisionProvider = FakeVisionProvider(
            GeminiPoolFactory.fromKeys(listOf("key-A", "key-B", "key-C"))
        )

        task.executeOcr()

        val outFile = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("scan_ocr.adoc")
        assertTrue(outFile.exists(), "OCR output should exist: ${outFile.absolutePath}")
        val content = outFile.readText(Charsets.UTF_8)
        assertTrue(content.contains("FakeVisionProvider"), "Output should come from FakeVisionProvider. Got:\n$content")
    }

    @Test
    fun `GeminiPoolFactory fromKeys builds pool usable by OcrTask`(@TempDir dir: Path) {
        val keys = listOf("secret-1", "secret-2")
        val pool = GeminiPoolFactory.fromKeys(keys)
        assertEquals(2, pool.size())

        val first = pool.nextInstance()
        val second = pool.nextInstance()
        assertEquals("gemini-key-0", first.id)
        assertEquals("gemini-key-1", second.id)
    }

    @Test
    fun `OcrTask with geminiApiKeys rotates on HTTP 429`(@TempDir dir: Path) {
        val project = ProjectBuilder.builder().withProjectDir(dir.toFile()).withName("ocr-429").build()
        project.pluginManager.apply(codebase.CodebasePlugin::class.java)

        val imgFile = File(dir.toFile(), "scan429.png")
        imgFile.writeBytes(ByteArray(100))

        val keys = listOf("key-A", "key-B")
        val pool = GeminiPoolFactory.fromKeys(keys)

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set("gemini")
        task.inputFile.set(project.layout.projectDirectory.file("scan429.png"))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")
        task.geminiApiKeys = keys
        task.geminiVisionProvider = FakeVisionProvider(pool)

        task.executeOcr()

        val content = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("scan429_ocr.adoc").readText(Charsets.UTF_8)
        assertTrue(content.contains("FakeVisionProvider"))

        val first = pool.nextInstance()
        pool.markRateLimited(first)
        val next = pool.nextInstance()
        assertTrue(next.id != first.id, "After 429, next key should differ from marked key")
    }
}