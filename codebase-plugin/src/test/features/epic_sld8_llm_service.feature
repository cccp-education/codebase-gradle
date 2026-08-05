@epic_sld8_llm_service
Feature: SLD-8 LLM BuildService — Gradle bridge for LlmProviderResolver
  As an N2 borough consumer (slider being the first)
  I want codebase to expose LlmProviderResolver as a Gradle BuildService
  So that I can inject an LlmProvider into my plugin tasks via Provider<LlmBuildService>

  Background:
    Given a Gradle project with sharedServices available

  @registration
  Scenario: Register LlmBuildService with ollama model
    When I register LlmBuildService with model "ollama"
    Then the service is instantiated successfully
    And the provider is non-null

  @registration
  Scenario: Register LlmBuildService with gemini model
    When I register LlmBuildService with model "gemini"
    Then the service is instantiated successfully
    And the provider is non-null

  @resolution
  Scenario: Resolve provider for custom model name
    When I register LlmBuildService with model "gpt-oss:120b-cloud"
    Then the service is instantiated successfully
    And the provider is non-null

  @resolution
  Scenario: Blank model falls back to ollama
    When I register LlmBuildService with model ""
    Then the service is instantiated successfully
    And the provider is non-null