@epic_v6_ollama_cloud
Feature: Ollama Cloud LLM — quota exceeded rotation
  As a codebase-gradle developer
  I want the LLM call to rotate to the next Ollama cloud instance when quota is exceeded
  So that the vibecoding loop stays resilient without manual intervention

  Background:
    Given an Ollama cloud pool with 2 instances

  @quota @rotation
  Scenario: LLM call rotates when the first instance returns quota exceeded
    Given the first Ollama instance returns "quota exceeded"
    When the LLM is called through the Ollama cloud provider
    Then the call succeeds on the second instance
    And the first instance is marked inactive or skipped

  @quota
  Scenario: LLM call throws when all cloud instances return quota exceeded
    Given every Ollama instance returns "quota exceeded"
    When the LLM is called through the Ollama cloud provider
    Then an IllegalStateException is thrown with message "All Ollama instances failed"
