package codebase.scenarios.ocr

import codebase.CodebasePlugin
import codebase.koog.llm.CodexOcrEngineAdapter
import codebase.koog.llm.FakeOllamaOcrProvider
import codebase.koog.llm.FakeVisionProvider
import codebase.koog.llm.ThrowingVisionProvider
import codex.ocr.TesseractOcrEngine
import codebase.ocr.FakeOcrEngine
import codebase.ocr.OcrTask
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OcrSteps {

    private val log = LoggerFactory.getLogger(OcrSteps::class.java)
    private var tmpDir: File? = null
    private var lastOutputPath: File? = null
    private var foundTask: OcrTask? = null
    private var foundGroup: String? = null
    private lateinit var testFileName: String
    private lateinit var baseName: String

    @Given("an OCR test file {string} with text {string}")
    fun createOcrTestFile(filename: String, text: String) {
        tmpDir = Files.createTempDirectory("ocr-cucumber").toFile()
        val file = File(tmpDir, filename)
        file.writeText(text, Charsets.UTF_8)
        testFileName = filename
        baseName = file.nameWithoutExtension
    }

    @Given("the codebase plugin is applied")
    fun applyCodebasePlugin() {
        tmpDir = Files.createTempDirectory("ocr-cucumber").toFile()
        testFileName = "dummy.txt"
        baseName = "dummy"
    }

    @When("I OCR {string} in French")
    fun ocrInFrench(filename: String) = ocr(filename, "fr", "asciidoc")

    @When("I OCR {string} in English")
    fun ocrInEnglish(filename: String) = ocr(filename, "en", "asciidoc")

    @When("I OCR {string} in French with format {string}")
    fun ocrWithFormat(filename: String, format: String) = ocr(filename, "fr", format)

    @When("I check for task {string}")
    fun checkForTask(taskName: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("lookup")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)
        foundTask = project.tasks.findByName(taskName) as? OcrTask
        foundGroup = foundTask?.group
    }

    @Then("the OCR result for {string} exists")
    fun ocrResultExists(expectedBase: String) {
        val file = resolveOutputFile(expectedBase)
        assertTrue(file.exists(), "OCR output should exist: ${file.absolutePath}")
    }

    @Then("the OCR result for {string} contains {string}")
    fun ocrResultContains(expectedBase: String, text: String) {
        val file = resolveOutputFile(expectedBase)
        val content = file.readText(Charsets.UTF_8)
        assertTrue(content.contains(text), "Expected '$text' in OCR output. Got:\n$content")
    }

    @Then("the OCR result for {string} ends with {string}")
    fun ocrResultEndsWith(expectedBase: String, suffix: String) {
        val file = resolveOutputFile(expectedBase)
        assertTrue(file.name.endsWith(suffix), "Expected suffix '$suffix': ${file.name}")
    }

    private fun resolveOutputFile(expectedBase: String): File {
        if (lastOutputPath != null) return lastOutputPath!!
        if (batchOutputDir != null) return batchOutputDir!!.resolve("${expectedBase}_ocr.adoc")
        throw AssertionError("No OCR output path recorded")
    }

    @Then("task {string} should be registered")
    fun taskShouldBeRegistered(taskName: String) {
        assertNotNull(foundTask, "Task '$taskName' should be registered")
    }

    @Then("task {string} should be in group {string}")
    fun taskShouldBeInGroup(taskName: String, group: String) {
        assertNotNull(foundTask, "Task '$taskName' should exist")
        assertEquals(group, foundGroup, "Task group mismatch")
    }

    @When("I OCR {string} with provider {string}")
    fun ocrWithProvider(filename: String, provider: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-provider")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val inputFile = File(tmpDir, filename)
        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set(provider)
        task.inputFile.set(project.layout.projectDirectory.file(filename))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")

        when (provider) {
            "ollama" -> {
                task.ollamaOcrProvider = FakeOllamaOcrProvider()
            }
            "gemini+ollama" -> {
                task.geminiVisionProvider = FakeVisionProvider()
                task.ollamaOcrProvider = FakeOllamaOcrProvider()
            }
            "tesseract" -> {
                task.tesseractOcrProvider = CodexOcrEngineAdapter(TesseractOcrEngine(tesseractPath = "tesseract"))
            }
        }

        task.executeOcr()

        val ext = ".adoc"
        lastOutputPath = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("${inputFile.nameWithoutExtension}_ocr$ext")
    }

    @When("I OCR {string} with provider {string} and Gemini fails")
    fun ocrWithProviderAndGeminiFails(filename: String, provider: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-gemini-fail")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val inputFile = File(tmpDir, filename)
        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set(provider)
        task.inputFile.set(project.layout.projectDirectory.file(filename))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")
        task.geminiVisionProvider = ThrowingVisionProvider()
        task.ollamaOcrProvider = FakeOllamaOcrProvider()

        task.executeOcr()

        val ext = ".adoc"
        lastOutputPath = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("${inputFile.nameWithoutExtension}_ocr$ext")
    }

    @When("I OCR {string} with provider {string} and Gemini and Ollama fail")
    fun ocrWithProviderAndGeminiAndOllamaFail(filename: String, provider: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-tess-fb")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val inputFile = File(tmpDir, filename)
        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrProvider.set(provider)
        task.inputFile.set(project.layout.projectDirectory.file(filename))
        task.ocrLanguage.set("eng")
        task.outputFormat.set("asciidoc")
        task.geminiVisionProvider = ThrowingVisionProvider()
        task.ollamaOcrProvider = ThrowingVisionProvider()
        task.tesseractOcrProvider = CodexOcrEngineAdapter(TesseractOcrEngine(tesseractPath = "tesseract"))

        task.executeOcr()

        val ext = ".adoc"
        lastOutputPath = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("${inputFile.nameWithoutExtension}_ocr$ext")
    }

    // ── OCR-3 : Batch inputDir ─────────────────────────────────────────

    private var batchOutputDir: File? = null
    private var lastError: Exception? = null

    @Given("an OCR test directory {string} with files:")
    fun createOcrTestDirectory(dirName: String, table: io.cucumber.datatable.DataTable) {
        tmpDir = Files.createTempDirectory("ocr-cucumber").toFile()
        val dir = File(tmpDir, dirName)
        dir.mkdirs()
        for (row in table.asLists()) {
            if (row.size < 2) continue
            val name = row[0]
            val content = row[1]
            File(dir, name).writeText(content, Charsets.UTF_8)
        }
    }

    @Given("an OCR test directory {string} with no files")
    fun createEmptyOcrTestDirectory(dirName: String) {
        tmpDir = Files.createTempDirectory("ocr-cucumber").toFile()
        File(tmpDir, dirName).mkdirs()
    }

    @When("I OCR the directory {string} in French")
    fun ocrDirectoryInFrench(dirName: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-batch")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = FakeVisionProvider()
        task.inputDir.set(project.layout.projectDirectory.dir(dirName))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")

        try {
            task.executeOcr()
            batchOutputDir = project.layout.buildDirectory.dir("ocr").get().asFile
        } catch (e: Exception) {
            lastError = e
        }
    }

    @When("I OCR file {string} with directory {string} in French")
    fun ocrFileWithDirectory(filename: String, dirName: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-mixed")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrEngine = FakeOcrEngine()
        task.geminiVisionProvider = FakeVisionProvider()
        task.inputFile.set(project.layout.projectDirectory.file(filename))
        task.inputDir.set(project.layout.projectDirectory.dir(dirName))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")

        task.executeOcr()
        batchOutputDir = project.layout.buildDirectory.dir("ocr").get().asFile
    }

    @Then("the OCR result for {string} does not exist")
    fun ocrResultDoesNotExist(expectedBase: String) {
        val file = batchOutputDir!!.resolve("${expectedBase}_ocr.adoc")
        assertTrue(!file.exists(), "OCR output should NOT exist: ${file.absolutePath}")
    }

    @Then("an error is raised with message containing {string}")
    fun errorRaisedWithMessage(expectedText: String) {
        assertNotNull(lastError, "Expected an error to be raised")
        assertTrue(lastError!!.message!!.contains(expectedText),
            "Error message should contain '$expectedText'. Got: ${lastError!!.message}")
    }

    @When("I OCR {string} in French with anonymization enabled")
    fun ocrInFrenchWithAnonymization(filename: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-anonymize")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val inputFile = File(tmpDir, filename)
        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrEngine = FakeOcrEngine()
        task.inputFile.set(project.layout.projectDirectory.file(filename))
        task.ocrLanguage.set("fr")
        task.outputFormat.set("asciidoc")
        task.anonymizeOutput.set(true)
        task.executeOcr()

        lastOutputPath = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("${inputFile.nameWithoutExtension}_ocr.adoc")
    }

    @Then("the OCR result for {string} does not contain {string}")
    fun ocrResultDoesNotContain(expectedBase: String, text: String) {
        val file = resolveOutputFile(expectedBase)
        val content = file.readText(Charsets.UTF_8)
        assertTrue(!content.contains(text), "OCR output should NOT contain '$text'. Got:\n$content")
    }

    private var metricsReportPath: File? = null

    @Then("the OCR metrics report exists")
    fun ocrMetricsReportExists() {
        val reportFile = batchOutputDir?.parentFile?.resolve("reports/ocr/ocr-metrics.adoc")
            ?: lastOutputPath?.parentFile?.parentFile?.resolve("reports/ocr/ocr-metrics.adoc")
        assertNotNull(reportFile, "Cannot resolve metrics report path")
        assertTrue(reportFile.exists(), "OCR metrics report should exist: ${reportFile.absolutePath}")
        metricsReportPath = reportFile
    }

    @Then("the OCR metrics report contains {string}")
    fun ocrMetricsReportContains(text: String) {
        assertNotNull(metricsReportPath, "Metrics report path should be set")
        val content = metricsReportPath!!.readText(Charsets.UTF_8)
        assertTrue(content.contains(text), "Metrics report should contain '$text'. Got:\n$content")
    }

    @After
    fun cleanup() {
        tmpDir?.deleteRecursively()
    }

    private fun ocr(filename: String, language: String, format: String) {
        val project = ProjectBuilder.builder()
            .withProjectDir(tmpDir!!)
            .withName("ocr-exec")
            .build()
        project.pluginManager.apply(CodebasePlugin::class.java)

        val inputFile = File(tmpDir, filename)
        val task = project.tasks.getByName("ocrDocument") as OcrTask
        task.ocrEngine = FakeOcrEngine()
        task.inputFile.set(project.layout.projectDirectory.file(filename))
        task.ocrLanguage.set(language)
        task.outputFormat.set(format)
        task.executeOcr()

        val ext = when (format) {
            "markdown" -> ".md"
            "text" -> ".txt"
            else -> ".adoc"
        }
        lastOutputPath = project.layout.buildDirectory.dir("ocr").get().asFile
            .resolve("${inputFile.nameWithoutExtension}_ocr$ext")
    }
}
