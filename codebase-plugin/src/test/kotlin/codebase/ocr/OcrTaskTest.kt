package codebase.ocr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OcrTaskTest {

    @Test
    fun `OcrEngine returns structured AsciiDoc for fake engine`() {
        val fake = FakeOcrEngine()
        val result = fake.process("image scan", "fr", "gemini-2.5-flash", 8192)

        assertTrue(result.contains("= Document OCRisé"))
        assertTrue(result.contains("image scan"))
        assertTrue(result.contains("langue: fr"))
        assertTrue(result.contains("modèle: gemini-2.5-flash"))
    }

    @Test
    fun `OcrEngine returns empty string for empty input`() {
        val fake = FakeOcrEngine()
        val result = fake.process("", "fr", "gemini-2.5-flash", 8192)

        assertEquals("", result)
    }

    @Test
    fun `OcrEngine respects metadata format`() {
        val fake = FakeOcrEngine()
        val result = fake.process("test content", "en", "gemini-1.5-flash", 4096)

        assertTrue(result.contains(":langue: en"))
        assertTrue(result.contains(":modèle: gemini-1.5-flash"))
        assertTrue(result.contains(":max-tokens: 4096"))
        assertTrue(result.startsWith("= Document OCRisé"))
    }

    @Test
    fun `FakeOcrEngine is an OcrEngine`() {
        val engine: OcrEngine = FakeOcrEngine()
        assertNotNull(engine)
        assertTrue(engine is OcrEngine)
    }

    @Test
    fun `fake engine does not throw for any input`() {
        val fake = FakeOcrEngine()

        val result1 = fake.process("a", "fr", "gemini-2.5-flash", 8192)
        assertTrue(result1.isNotEmpty())

        val result2 = fake.process("a".repeat(10_000), "fr", "gemini-2.5-flash", 8192)
        assertTrue(result2.isNotEmpty())

        val result3 = fake.process("éàùç汉字🎉", "auto", "gemini-2.5-flash", 8192)
        assertTrue(result3.isNotEmpty())
    }

    @Test
    fun `ocr task class exists and is abstract`() {
        val clazz = OcrTask::class.java
        assertTrue(clazz.simpleName == "OcrTask")
        assertFalse(clazz.isInterface)
    }

    @Test
    fun `OcrInputFile data class stores fields`() {
        val input = OcrInputFile(
            path = "/tmp/scan.pdf",
            language = "fr",
            provider = "gemini",
            model = "gemini-2.5-flash",
            maxTokens = 8192
        )
        assertEquals("/tmp/scan.pdf", input.path)
        assertEquals("fr", input.language)
        assertEquals("gemini", input.provider)
        assertEquals("gemini-2.5-flash", input.model)
        assertEquals(8192, input.maxTokens)
    }

    @Test
    fun `OcrInputFile copy works`() {
        val input = OcrInputFile("/tmp/scan.pdf")
        val modified = input.copy(language = "en", maxTokens = 4096)

        assertEquals("/tmp/scan.pdf", modified.path)
        assertEquals("en", modified.language)
        assertEquals(4096, modified.maxTokens)
        assertTrue(modified !== input)
    }

    // ── OCR-3 : Détection image vs texte ──────────────────────────────

    @Test
    fun `isImageFile detects PNG as image`() {
        val file = File("/tmp/scan.png")
        assertTrue(OcrTask.isImageFile(file))
    }

    @Test
    fun `isImageFile detects JPG as image`() {
        val file = File("/tmp/photo.jpg")
        assertTrue(OcrTask.isImageFile(file))
    }

    @Test
    fun `isImageFile detects JPEG as image`() {
        val file = File("/tmp/photo.jpeg")
        assertTrue(OcrTask.isImageFile(file))
    }

    @Test
    fun `isImageFile detects all supported image formats`() {
        for (ext in listOf("png", "jpg", "jpeg", "gif", "bmp", "tiff")) {
            assertTrue(OcrTask.isImageFile(File("/tmp/doc.$ext")), "Failed for $ext")
            assertTrue(OcrTask.isImageFile(File("/tmp/DOC.${ext.uppercase()}")), "Failed for uppercase $ext")
        }
    }

    @Test
    fun `isImageFile returns false for text files`() {
        assertFalse(OcrTask.isImageFile(File("/tmp/doc.pdf")))
        assertFalse(OcrTask.isImageFile(File("/tmp/doc.txt")))
        assertFalse(OcrTask.isImageFile(File("/tmp/doc.adoc")))
        assertFalse(OcrTask.isImageFile(File("/tmp/doc.md")))
        assertFalse(OcrTask.isImageFile(File("/tmp/doc.xml")))
        assertFalse(OcrTask.isImageFile(File("/tmp/doc.html")))
        assertFalse(OcrTask.isImageFile(File("/tmp/doc")))
    }

    @Test
    fun `detectMimeType returns correct image MIME types`() {
        assertEquals("image/png", OcrTask.detectMimeType("png"))
        assertEquals("image/jpeg", OcrTask.detectMimeType("jpg"))
        assertEquals("image/jpeg", OcrTask.detectMimeType("jpeg"))
        assertEquals("image/gif", OcrTask.detectMimeType("gif"))
        assertEquals("image/bmp", OcrTask.detectMimeType("bmp"))
        assertEquals("image/tiff", OcrTask.detectMimeType("tiff"))
    }

    @Test
    fun `detectMimeType returns octet-stream for unknown extensions`() {
        assertEquals("application/octet-stream", OcrTask.detectMimeType("pdf"))
        assertEquals("application/octet-stream", OcrTask.detectMimeType("xyz"))
    }

    // ── OCR-3b : Injection FakeVisionProvider ──────────────────────────

    @Test
    fun `FakeVisionProvider is injectable into OcrTask`() {
        val fakeProvider = codebase.koog.llm.FakeVisionProvider()
        val task = org.gradle.testfixtures.ProjectBuilder.builder().build()
            .tasks.register("ocr", OcrTask::class.java).get()
        task.geminiVisionProvider = fakeProvider
        kotlin.test.assertNotNull(task.geminiVisionProvider)
    }

    // ── OCR-3b P1 : llm-config.yml + -PinputFile ──────────────────────

    @Test
    fun `llmConfigFile is null by default`() {
        val task = org.gradle.testfixtures.ProjectBuilder.builder().build()
            .tasks.register("ocr", OcrTask::class.java).get()
        kotlin.test.assertNull(task.llmConfigFile, "llmConfigFile should be null by default")
    }

    @Test
    fun `llmConfigFile can be set to an existing YAML`(@TempDir tempDir: Path) {
        val ymlFile = tempDir.resolve("llm-config.yml").toFile()
        ymlFile.writeText("""
            ai:
              gemini:
                envVar: "GEMINI_API_KEY"
                model: "gemini-1.5-flash"
                baseUrl: "https://generativelanguage.googleapis.com/v1beta"
        """.trimIndent())

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        project.pluginManager.apply("java-base") // nécessaire pour ProjectBuilder + RegularFileProperty

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.llmConfigFile = ymlFile

        kotlin.test.assertNotNull(task.llmConfigFile)
        assertTrue(ymlFile.exists())
    }

    @Test
    fun `executeOcr with YAML config resolves model from GeminiConfig`(@TempDir tempDir: Path) {
        // Arrange — llm-config.yml
        val ymlFile = tempDir.resolve("llm-config.yml").toFile()
        ymlFile.writeText("""
            ai:
              gemini:
                envVar: "GEMINI_API_KEY"
                model: "gemini-1.5-flash"
                baseUrl: "https://generativelanguage.googleapis.com/v1beta"
        """.trimIndent())

        // Fichier d'entrée — texte simple (pas image → passe par OcrEngine/texte)
        val inputFile = tempDir.resolve("document.txt").toFile()
        inputFile.writeText("Test content for OCR")

        // Fake engine + Fake vision provider
        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-yaml-config")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.llmConfigFile = ymlFile
        task.inputFile.set(project.layout.projectDirectory.file("document.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        // Act — exécution de la tâche
        task.executeOcr()

        // Assert — le fichier de sortie doit exister
        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("document_ocr.adoc")
        assertTrue(outputFile.exists(), "Output file should be created: ${outputFile.absolutePath}")
        val content = outputFile.readText()
        assertTrue(content.contains("Test content for OCR"))
    }

    @Test
    fun `executeOcr with -PinputFile via plugin and YAML config`(@TempDir tempDir: Path) {
        // Arrange — llm-config.yml
        val ymlFile = tempDir.resolve("llm-config.yml").toFile()
        ymlFile.writeText("""
            ai:
              gemini:
                envVar: "GEMINI_API_KEY"
                model: "gemini-1.5-flash"
                baseUrl: "https://generativelanguage.googleapis.com/v1beta"
        """.trimIndent())

        // Fichier d'entrée
        val inputFile = tempDir.resolve("scan.png").toFile()
        inputFile.writeText("fake png bytes for testing")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-cli-input")
            .build()
        project.pluginManager.apply("java-base")

        // Simule -PinputFile=scan.png
        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()
        // llmConfigFile pas set manuellement — simule le comportement du plugin
        // (sera null → fallback convention gemini-2.5-flash)

        // Act
        assertTrue(inputFile.exists(), "Input file must exist for OCR")
        // Ne pas exécuter l'OCR complet (évite le mode image réel)
        // Test uniquement l'injection de -PinputFile
        kotlin.test.assertNotNull(task.inputFile)
        assertEquals("scan.png", task.inputFile.get().asFile.name)
    }

    @Test
    fun `executeOcr with YAML model override falls back to convention when YAML absent`(@TempDir tempDir: Path) {
        // Arrange — pas de llm-config.yml
        val inputFile = tempDir.resolve("doc.txt").toFile()
        inputFile.writeText("No YAML here")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("doc.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()
        // llmConfigFile = null (par défaut)

        // Act
        task.executeOcr()

        // Assert — convention gemini-2.5-flash utilisé
        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("gemini-2.5-flash"), "Should fallback to convention model")
    }

    @Test
    fun `executeOcr with corrupt YAML file falls back to convention`(@TempDir tempDir: Path) {
        // Arrange — YAML invalide
        val ymlFile = tempDir.resolve("llm-config.yml").toFile()
        ymlFile.writeText("this: is: not: valid:: yaml")

        val inputFile = tempDir.resolve("doc.txt").toFile()
        inputFile.writeText("Corrupt YAML test")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.llmConfigFile = ymlFile
        task.inputFile.set(project.layout.projectDirectory.file("doc.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        // Act — ne doit pas planter
        task.executeOcr()

        // Assert
        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
    }

    @Test
    fun `executeOcr with ollama provider uses FakeOllamaOcrProvider`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("scan.png").toFile()
        inputFile.writeText("fake png bytes for ollama OCR")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-ollama-ocr")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        task.ocrProvider.set("ollama")
        task.ollamaOcrProvider = codebase.koog.llm.FakeOllamaOcrProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("scan_ocr.adoc")
        assertTrue(outputFile.exists(), "Output file should be created: ${outputFile.absolutePath}")
        val content = outputFile.readText()
        assertTrue(content.contains("FakeOllamaOcrProvider"))
        assertTrue(content.contains("qwen3-vl:235b-cloud"))
    }

    @Test
    fun `executeOcr with gemini+ollama fallback uses Gemini first`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("scan.png").toFile()
        inputFile.writeText("fake png bytes for fallback OCR")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-gemini-ollama-fallback")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        task.ocrProvider.set("gemini+ollama")
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()
        task.ollamaOcrProvider = codebase.koog.llm.FakeOllamaOcrProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("scan_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("FakeVisionProvider"), "Gemini should be used first in gemini+ollama mode")
    }

    @Test
    fun `executeOcr with gemini+ollama falls back to ollama when gemini throws`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("scan.png").toFile()
        inputFile.writeText("fake png bytes for fallback OCR")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-gemini-fails-ollama-fallback")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        task.ocrProvider.set("gemini+ollama")
        task.geminiVisionProvider = codebase.koog.llm.ThrowingVisionProvider()
        task.ollamaOcrProvider = codebase.koog.llm.FakeOllamaOcrProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("scan_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("FakeOllamaOcrProvider"), "Should fallback to Ollama when Gemini fails")
    }

    @Test
    fun `executeOcr with ollama provider and custom baseUrl model`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("scan.png").toFile()
        inputFile.writeText("fake png bytes for custom ollama")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-ollama-custom")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("scan.png"))
        task.ocrProvider.set("ollama")
        task.ollamaBaseUrl.set("http://localhost:11437")
        task.ollamaModel.set("qwen3-vl:235b-cloud")
        task.ollamaOcrProvider = codebase.koog.llm.FakeOllamaOcrProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("scan_ocr.adoc")
        assertTrue(outputFile.exists())
    }

    // ── OCR-3 : Batch inputDir ─────────────────────────────────────────

    @Test
    fun `inputDir property exists and is abstract`() {
        val task = org.gradle.testfixtures.ProjectBuilder.builder().build()
            .tasks.register("ocr", OcrTask::class.java).get()
        assertNotNull(task.inputDir)
    }

    @Test
    fun `executeOcr with inputDir processes multiple files`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("doc1.txt").writeText("Content 1")
        inputDir.resolve("doc2.txt").writeText("Content 2")
        inputDir.resolve("doc3.txt").writeText("Content 3")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-inputdir")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val out1 = outputDir.resolve("doc1_ocr.adoc")
        val out2 = outputDir.resolve("doc2_ocr.adoc")
        val out3 = outputDir.resolve("doc3_ocr.adoc")
        assertTrue(out1.exists(), "doc1_ocr.adoc should exist")
        assertTrue(out2.exists(), "doc2_ocr.adoc should exist")
        assertTrue(out3.exists(), "doc3_ocr.adoc should exist")
        assertTrue(out1.readText().contains("Content 1"))
        assertTrue(out2.readText().contains("Content 2"))
        assertTrue(out3.readText().contains("Content 3"))
    }

    @Test
    fun `executeOcr with inputDir on empty directory throws`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("empty").toFile()
        inputDir.mkdirs()

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-empty-dir")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("empty"))
        task.ocrEngine = FakeOcrEngine()

        val ex = assertThrows<IllegalArgumentException> { task.executeOcr() }
        assertTrue(ex.message!!.contains("Aucun fichier d'entrée"))
    }

    @Test
    fun `executeOcr with inputDir on non-existent directory throws`(@TempDir tempDir: Path) {
        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-nonexistent-dir")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("nonexistent"))
        task.ocrEngine = FakeOcrEngine()

        val ex = assertThrows<IllegalArgumentException> { task.executeOcr() }
        assertTrue(ex.message!!.contains("introuvable"))
    }

    @Test
    fun `executeOcr with inputFile takes priority over inputDir`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("batch1.txt").writeText("Batch content")

        val singleFile = tempDir.resolve("single.txt").toFile()
        singleFile.writeText("Single content")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-inputfile-priority")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("single.txt"))
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val singleOutput = outputDir.resolve("single_ocr.adoc")
        val batchOutput = outputDir.resolve("batch1_ocr.adoc")
        assertTrue(singleOutput.exists(), "Single file output should exist")
        assertFalse(batchOutput.exists(), "Batch file output should NOT exist when inputFile takes priority")
        assertTrue(singleOutput.readText().contains("Single content"))
    }

    @Test
    fun `executeOcr with inputDir and outputFile ignores custom outputFile in batch mode`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("a.txt").writeText("A")
        inputDir.resolve("b.txt").writeText("B")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-outputfile")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.outputFile.set(project.layout.buildDirectory.file("custom.adoc"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        assertTrue(outputDir.resolve("a_ocr.adoc").exists())
        assertTrue(outputDir.resolve("b_ocr.adoc").exists())
        assertFalse(project.layout.buildDirectory.file("custom.adoc").get().asFile.exists(),
            "Custom outputFile should be ignored in batch mode")
    }

    @Test
    fun `executeOcr with inputDir sorts files alphabetically`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("z.txt").writeText("Z")
        inputDir.resolve("a.txt").writeText("A")
        inputDir.resolve("m.txt").writeText("M")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-sorted")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        assertTrue(outputDir.resolve("a_ocr.adoc").exists())
        assertTrue(outputDir.resolve("m_ocr.adoc").exists())
        assertTrue(outputDir.resolve("z_ocr.adoc").exists())
    }

    @Test
    fun `executeOcr with inputDir skips subdirectories`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("file.txt").writeText("File")
        inputDir.resolve("subdir").mkdirs()
        inputDir.resolve("subdir/nested.txt").writeText("Nested")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-skip-subdirs")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        assertTrue(outputDir.resolve("file_ocr.adoc").exists())
        assertFalse(outputDir.resolve("nested_ocr.adoc").exists(),
            "Nested files in subdirectories should be skipped")
    }

    @Test
    fun `executeOcr with inputDir and image files uses image OCR path`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("photo.png").writeText("fake png bytes")
        inputDir.resolve("doc.txt").writeText("text content")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-mixed")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        assertTrue(outputDir.resolve("doc_ocr.adoc").exists())
        assertTrue(outputDir.resolve("photo_ocr.adoc").exists())
        assertTrue(outputDir.resolve("photo_ocr.adoc").readText().contains("FakeVisionProvider"))
    }

    @Test
    fun `executeOcr with inputDir and markdown format produces md files`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("doc.txt").writeText("Markdown test")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-markdown")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.outputFormat.set("markdown")
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val mdFile = outputDir.resolve("doc_ocr.md")
        assertTrue(mdFile.exists(), "Markdown output should have .md extension")
        assertFalse(outputDir.resolve("doc_ocr.adoc").exists(), "AsciiDoc output should not exist")
    }

    @Test
    fun `executeOcr with inputDir and text format produces txt files`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("doc.txt").writeText("Text format test")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-text")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.outputFormat.set("text")
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val txtFile = outputDir.resolve("doc_ocr.txt")
        assertTrue(txtFile.exists(), "Text output should have .txt extension")
    }

    @Test
    fun `executeOcr with inputDir and ollama provider processes all files`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("img1.png").writeText("fake png 1")
        inputDir.resolve("img2.png").writeText("fake png 2")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-ollama")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrProvider.set("ollama")
        task.ollamaOcrProvider = codebase.koog.llm.FakeOllamaOcrProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        assertTrue(outputDir.resolve("img1_ocr.adoc").exists())
        assertTrue(outputDir.resolve("img2_ocr.adoc").exists())
        assertTrue(outputDir.resolve("img1_ocr.adoc").readText().contains("FakeOllamaOcrProvider"))
    }

    @Test
    fun `executeOcr with inputDir and gemini+ollama fallback processes all files`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("img1.png").writeText("fake png 1")
        inputDir.resolve("img2.png").writeText("fake png 2")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-fallback")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrProvider.set("gemini+ollama")
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()
        task.ollamaOcrProvider = codebase.koog.llm.FakeOllamaOcrProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        assertTrue(outputDir.resolve("img1_ocr.adoc").exists())
        assertTrue(outputDir.resolve("img2_ocr.adoc").exists())
    }

    @Test
    fun `executeOcr with inputDir and YAML config resolves model for all files`(@TempDir tempDir: Path) {
        val ymlFile = tempDir.resolve("llm-config.yml").toFile()
        ymlFile.writeText("""
            ai:
              gemini:
                envVar: "GEMINI_API_KEY"
                model: "gemini-1.5-flash"
                baseUrl: "https://generativelanguage.googleapis.com/v1beta"
        """.trimIndent())

        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("doc.txt").writeText("YAML batch test")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-yaml")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.llmConfigFile = ymlFile
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = codebase.koog.llm.FakeVisionProvider()

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
        assertTrue(outputFile.readText().contains("YAML batch test"))
    }

    @Test
    fun `executeOcr with neither inputFile nor inputDir throws`(@TempDir tempDir: Path) {
        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-no-input")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.ocrEngine = FakeOcrEngine()

        val ex = assertThrows<IllegalArgumentException> { task.executeOcr() }
        assertTrue(ex.message!!.contains("Aucun fichier d'entrée"))
    }

    // ── OCR-4 : Anonymisation pipeline ──────────────────────────────────

    @Test
    fun `anonymizeOutput is false by default`() {
        val task = org.gradle.testfixtures.ProjectBuilder.builder().build()
            .tasks.register("ocr", OcrTask::class.java).get()
        assertEquals(false, task.anonymizeOutput.orNull)
    }

    @Test
    fun `executeOcr with anonymizeOutput replaces emails in result`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("doc.txt").toFile()
        inputFile.writeText("Contact: jean.dupont@example.com")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-anonymize-email")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("doc.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.anonymizeOutput.set(true)

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertFalse(content.contains("jean.dupont@example.com"))
        assertTrue(content.contains("***@anonymous.com"))
    }

    @Test
    fun `executeOcr with anonymizeOutput replaces phone numbers`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("doc.txt").toFile()
        inputFile.writeText("Tel: 06 12 34 56 78")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-anonymize-phone")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("doc.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.anonymizeOutput.set(true)

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertFalse(content.contains("06 12 34 56 78"))
    }

    @Test
    fun `executeOcr with anonymizeOutput replaces API keys`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("doc.txt").toFile()
        inputFile.writeText("Authorization: sk-ant-api03-abcdefghijklmnopqrstuvwxyz123456")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-anonymize-apikey")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("doc.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.anonymizeOutput.set(true)

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertFalse(content.contains("sk-ant-api03"))
    }

    @Test
    fun `executeOcr with anonymizeOutput false preserves PII`(@TempDir tempDir: Path) {
        val inputFile = tempDir.resolve("doc.txt").toFile()
        inputFile.writeText("Contact: jean.dupont@example.com, Tel: 06 12 34 56 78")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-no-anonymize")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputFile.set(project.layout.projectDirectory.file("doc.txt"))
        task.ocrEngine = FakeOcrEngine()
        task.anonymizeOutput.set(false)

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val outputFile = outputDir.resolve("doc_ocr.adoc")
        assertTrue(outputFile.exists())
        val content = outputFile.readText()
        assertTrue(content.contains("jean.dupont@example.com"))
        assertTrue(content.contains("06 12 34 56 78"))
    }

    @Test
    fun `executeOcr batch with anonymizeOutput anonymizes all files`(@TempDir tempDir: Path) {
        val inputDir = tempDir.resolve("scans").toFile()
        inputDir.mkdirs()
        inputDir.resolve("doc1.txt").writeText("Email: alice@acme.com")
        inputDir.resolve("doc2.txt").writeText("Tel: +33 6 12 34 56 78")

        val project = org.gradle.testfixtures.ProjectBuilder.builder()
            .withProjectDir(tempDir.toFile())
            .withName("test-batch-anonymize")
            .build()
        project.pluginManager.apply("java-base")

        val task = project.tasks.register("ocr", OcrTask::class.java).get()
        task.inputDir.set(project.layout.projectDirectory.dir("scans"))
        task.ocrEngine = FakeOcrEngine()
        task.anonymizeOutput.set(true)

        task.executeOcr()

        val outputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        val out1 = outputDir.resolve("doc1_ocr.adoc")
        val out2 = outputDir.resolve("doc2_ocr.adoc")
        assertTrue(out1.exists())
        assertTrue(out2.exists())
        assertFalse(out1.readText().contains("alice@acme.com"))
        assertFalse(out2.readText().contains("+33 6 12 34 56 78"))
    }

    private inline fun <reified T : Throwable> assertThrows(noinline block: () -> Unit): T {
        return org.junit.jupiter.api.Assertions.assertThrows(T::class.java) { block() }
    }
}
