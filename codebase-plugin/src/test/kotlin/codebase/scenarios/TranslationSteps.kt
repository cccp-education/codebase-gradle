package codebase.scenarios

import codebase.i18n.FakeLlmTranslator
import codebase.i18n.LlmTranslator
import codebase.koog.llm.FakeLlmProvider
import contracts.i18n.TranslationResult
import io.cucumber.java.en.Given
import io.cucumber.java.en.Then
import io.cucumber.java.en.When
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TranslationSteps(private val world: TranslationWorld) {

    @Given("a FakeLlmTranslator without enqueued results")
    fun `fake translator empty`() {
        world.reset()
        world.translator = FakeLlmTranslator()
    }

    @Given("a FakeLlmTranslator with one enqueued success {string}")
    fun `fake translator with success`(text: String) {
        world.reset()
        world.translator = FakeLlmTranslator().apply {
            enqueueResult(TranslationResult.Success(text))
        }
    }

    @Given("a FakeLlmTranslator with one enqueued failure {string}")
    fun `fake translator with failure`(reason: String) {
        world.reset()
        world.translator = FakeLlmTranslator().apply {
            enqueueResult(TranslationResult.Failure(reason))
        }
    }

    @Given("an LlmTranslator backed by a FakeLlmProvider returning {string}")
    fun `llm translator with fake provider`(response: String) {
        world.reset()
        val provider = FakeLlmProvider().apply { nextResponse = response }
        world.fakeLlmProvider = provider
        world.translator = LlmTranslator(provider)
    }

    @When("I request translation of {string} from {string} to {string}")
    fun `request translation`(sourceText: String, src: String, tgt: String) {
        world.executeTranslation(sourceText, src, tgt)
    }

    @When("I attempt to request translation of {string} from {string} to {string}")
    fun `attempt translation`(sourceText: String, src: String, tgt: String) {
        world.attemptTranslation(sourceText, src, tgt)
    }

    @Then("the translation result is a success")
    fun `result is success`() {
        assertNotNull(world.result, "result should not be null")
        assertIs<TranslationResult.Success>(world.result)
    }

    @Then("the translation result is a failure")
    fun `result is failure`() {
        assertNotNull(world.result, "result should not be null")
        assertIs<TranslationResult.Failure>(world.result)
    }

    @Then("the translated text is {string}")
    fun `translated text equals`(expected: String) {
        assertEquals(expected, world.successResult().translatedText)
    }

    @Then("the failure reason is {string}")
    fun `failure reason equals`(expected: String) {
        assertEquals(expected, world.failureResult().reason)
    }

    @Then("the translator recorded one request with source {string} and target {string}")
    fun `recorded request`(src: String, tgt: String) {
        val fake = world.fakeTranslator()
        assertEquals(1, fake.requestsReceived.size)
        val req = fake.requestsReceived.first()
        assertEquals(src, req.sourceLanguage)
        assertEquals(tgt, req.targetLanguage)
    }

    @Then("the request is rejected with an IllegalArgumentException")
    fun `rejected illegal argument`() {
        assertNotNull(world.caughtException, "exception should have been thrown")
        assertIs<IllegalArgumentException>(world.caughtException)
    }
}