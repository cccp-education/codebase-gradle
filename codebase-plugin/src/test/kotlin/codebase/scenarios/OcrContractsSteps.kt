package codebase.scenarios

import codebase.CodebasePlugin
import codebase.koog.llm.ThrowingVisionProvider
import codebase.koog.llm.adapter.VisionOcrEngineAdapter
import codebase.ocr.OcrTask
import contracts.ocr.OcrRequest
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Cucumber steps for `@ocr-contracts` scenarios (EPIC CDX-OCR-CONTRACTS US-4).
 *
 * Steps are prefixed "OCR contracts" / "N0 OCR engine" / "vision provider
 * stub" / "codebase OCR task" to avoid glue collisions with other feature
 * step classes sharing the `codebase.scenarios` package (pattern S-088).
 *
 * Pure BDD: no network call, no API key, no real Gradle execution — the
 * scenarios drive the N0 `contracts.ocr.OcrEngine` port via the real
 * [VisionOcrEngineAdapter] backed by a [StubVisionProvider], and the
 * `OcrTask` AI-only rejection path via `executeOcr` on a ProjectBuilder
 * fixture.
 */
class OcrContractsSteps(private val world: OcrContractsWorld) {

    @Given("an OCR contracts world is initialized")
    fun `ocr contracts world initialized`() {
        assertNotNull(world, "OcrContractsWorld should be instantiated by PicoContainer")
    }

    @When("the vision provider stub returns {string} for a PNG request in French")
    fun `vision provider stub returns`(response: String) {
        world.visionStub.resetQueue(listOf(response))
        world.scanFile = null
        pendingRequest = OcrRequest(
            imageData = byteArrayOf(1, 2, 3),
            format = "image/png",
            language = "fr"
        )
    }

    @Given("a JPEG request in English processed with model {string}")
    fun `jpeg request with model`(model: String) {
        adapterModel = model
        pendingRequest = OcrRequest(
            imageData = ByteArray(0),
            format = "image/jpeg",
            language = "en"
        )
    }

    @When("the N0 OCR engine adapter processes the request")
    fun `n0 engine processes request`() {
        val engine = VisionOcrEngineAdapter(world.visionStub, model = adapterModel)
        world.engine = engine
        world.ocrResult = engine.process(pendingRequest!!)
    }

    @Then("the OCR result structured text is {string}")
    fun `ocr result structured text`(expected: String) {
        assertEquals(expected, world.ocrResult!!.structuredText)
    }

    @And("the OCR result class is contracts.ocr.OcrResult")
    fun `ocr result class`() {
        assertEquals("contracts.ocr.OcrResult", world.ocrResult!!.javaClass.name)
    }

    @And("the OCR result confidence is {double}")
    fun `ocr result confidence`(expected: Double) {
        assertEquals(expected, world.ocrResult!!.confidence)
    }

    @Then("the OCR result language is {string}")
    fun `ocr result language`(expected: String) {
        assertEquals(expected, world.ocrResult!!.language)
    }

    @And("the OCR result source format is {string}")
    fun `ocr result source format`(expected: String) {
        assertEquals(expected, world.ocrResult!!.sourceFormat)
    }

    @And("the OCR result model is {string}")
    fun `ocr result model`(expected: String) {
        assertEquals(expected, world.ocrResult!!.model)
    }

    private lateinit var successiveRequests: List<OcrRequest>
    private var successiveResults: List<contracts.ocr.OcrResult> = emptyList()

    @Given("two successive requests through the same N0 engine instance")
    fun `two successive requests`() {
        world.visionStub.resetQueue(listOf("= Page un", "= Page two"))
        successiveRequests = listOf(
            OcrRequest(imageData = byteArrayOf(1), format = "image/png", language = "fr"),
            OcrRequest(imageData = byteArrayOf(2), format = "image/png", language = "en")
        )
    }

    @When("the N0 OCR engine adapter processes both requests")
    fun `n0 engine processes both requests`() {
        val engine = VisionOcrEngineAdapter(world.visionStub)
        world.engine = engine
        successiveResults = successiveRequests.map { engine.process(it) }
    }

    @Then("both OCR results carry their own request text")
    fun `both results carry own text`() {
        assertEquals(2, successiveResults.size)
        assertEquals("= Page un", successiveResults[0].structuredText)
        assertEquals("= Page two", successiveResults[1].structuredText)
    }

    @And("the vision provider stub was called {int} times")
    fun `stub called times`(expected: Int) {
        assertEquals(expected, world.visionStub.callCount)
    }

    // ── Degraded Tesseract boundary (OcrTask AI-only) ──────────────────

    @Given("a scan file {string} submitted with software provider {string}")
    fun `scan file with software provider`(filename: String, provider: String) {
        prepareTask(filename)
        task!!.ocrProvider.set(provider)
    }

    @Given("a scan file {string} with providers gemini and ollama both failing")
    fun `scan file both providers failing`(filename: String) {
        prepareTask(filename)
        task!!.ocrProvider.set("gemini+ollama")
        task!!.geminiVisionProvider = ThrowingVisionProvider()
        task!!.ollamaOcrProvider = ThrowingVisionProvider()
    }

    @When("the codebase OCR task processes the file")
    fun `codebase ocr task processes file`() {
        try {
            task!!.executeOcr()
        } catch (e: Exception) {
            world.taskError = e
        }
        world.outputDir = project!!
            .layout.buildDirectory.dir("ocr").get().asFile
    }

    @Then("an OCR contracts error is raised containing {string}")
    fun `ocr contracts error raised`(expected: String) {
        assertNotNull(world.taskError, "Expected an error to be raised")
        assertTrue(
            world.taskError!!.message!!.contains(expected),
            "Error message should contain '$expected'. Got: ${world.taskError!!.message}"
        )
    }

    @And("no OCR output file exists for {string}")
    fun `no ocr output file`(baseName: String) {
        val output = world.outputDir!!.resolve("${baseName}_ocr.adoc")
        assertTrue(!output.exists(), "OCR output should NOT exist: ${output.absolutePath}")
    }

    // ── Internals ──────────────────────────────────────────────────────

    private var pendingRequest: OcrRequest? = null
    private var adapterModel: String = "gemini-2.5-flash"

    private var project: org.gradle.api.Project? = null
    private var task: OcrTask? = null

    private fun prepareTask(filename: String) {
        val tmpDir = Files.createTempDirectory("ocr-contracts").toFile()
        val imgFile = File(tmpDir, filename)
        imgFile.writeBytes(minimalPng())
        val proj = ProjectBuilder.builder()
            .withProjectDir(tmpDir)
            .withName("ocr-contracts-task")
            .build()
        proj.pluginManager.apply(CodebasePlugin::class.java)
        val t = proj.tasks.getByName("ocrDocument") as OcrTask
        t.inputFile.set(proj.layout.projectDirectory.file(filename))
        t.ocrLanguage.set("eng")
        t.outputFormat.set("asciidoc")
        project = proj
        task = t
    }

    private fun minimalPng(): ByteArray {
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
