@epic_v6_pool
Feature: Ollama Cloud Pool — gemma4:31b-cloud integration
  As a codebase-gradle developer
  I want the Ollama cloud pool to include gemma4:31b-cloud in its model rotation
  So that the vibecoding loop can leverage both authorized cloud models

  Background:
    Given no Ollama scan environment variables are set

  @gemma4 @factory
  Scenario: Factory pool includes gemma4:31b-cloud in authorized models
    When I resolve provider for model "ollama"
    Then the provider is an OllamaLlmProvider
    And the provider pool contains 29 instances
    And the provider pool models cycle through the 2 authorized cloud models
    And the provider pool includes model "gemma4:31b-cloud"

  @gemma4 @explicit
  Scenario: Explicit OLLAMA_POOL_PORTS cycles gemma4:31b-cloud across ports
    Given OLLAMA_POOL_PORTS is set to "11450,11451,11452"
    When I resolve provider for model "ollama"
    Then the provider is an OllamaLlmProvider
    And the provider pool models cycle through the 2 authorized cloud models
    And the provider pool includes model "gemma4:31b-cloud"

  @gemma4 @vibecoding
  Scenario: Vibecoding pool with gemma4:31b-cloud rotates on quota exceeded
    Given an Ollama cloud pool with 2 instances
    And the first Ollama instance returns "quota exceeded"
    When the LLM is called through the Ollama cloud provider
    Then the call succeeds on the second instance
    And the first instance is marked inactive or skipped