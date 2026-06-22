@epic_translation
Feature: TranslationService — Cross-Borough LLM Translation (EPIC TRAD)
  As a bakery-gradle consumer (US-I18N-MIG-4)
  I want codebase-gradle to expose a TranslationService that wraps an LLM provider
  So that messages_{lang}.properties can be filled automatically without manual translation

  @translation_success
  Scenario: FakeLlmTranslator returns deterministic stub translation
    Given a FakeLlmTranslator without enqueued results
    When I request translation of "Bonjour le monde" from "fr" to "en"
    Then the translation result is a success
    And the translated text is "[en] Bonjour le monde"
    And the translator recorded one request with source "fr" and target "en"

  @translation_enqueued
  Scenario: Enqueued result is returned FIFO
    Given a FakeLlmTranslator with one enqueued success "Hello world"
    When I request translation of "Bonjour le monde" from "fr" to "en"
    Then the translation result is a success
    And the translated text is "Hello world"

  @translation_failure
  Scenario: Enqueued failure is propagated
    Given a FakeLlmTranslator with one enqueued failure "quota exceeded"
    When I request translation of "Merci" from "fr" to "en"
    Then the translation result is a failure
    And the failure reason is "quota exceeded"

  @translation_llm_success
  Scenario: LlmTranslator wraps LlmProvider and sanitizes quotes
    Given an LlmTranslator backed by a FakeLlmProvider returning "  \"Saludos\"  "
    When I request translation of "Bonjour" from "fr" to "es"
    Then the translation result is a success
    And the translated text is "Saludos"

  @translation_llm_blank
  Scenario: LlmTranslator returns failure on blank LLM response
    Given an LlmTranslator backed by a FakeLlmProvider returning "   "
    When I request translation of "Bonjour" from "fr" to "en"
    Then the translation result is a failure

  @translation_rejects_same_language
  Scenario: Request with same source and target language is rejected
    Given a FakeLlmTranslator without enqueued results
    When I attempt to request translation of "Hello" from "en" to "en"
    Then the request is rejected with an IllegalArgumentException