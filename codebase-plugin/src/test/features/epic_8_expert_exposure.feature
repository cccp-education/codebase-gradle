@epic8 @wip
Feature: EPIC 8 - Exposer experts via Ollama

  As a plugin developer (slider/plantuml/bakery)
  I want a manifest JSON describing available expert models and their Ollama endpoints
  So that I can consume domain-specific LLM agents without hardcoding URLs

  Background:
    Given an expert exposure registry with "kotlin" and "docs" domains

  Scenario: Task is registered by CodebasePlugin
    Given the codebase plugin is applied for expert exposure
    When I check for exposure task "exposeExperts"
    Then exposure task "exposeExperts" should be registered
    And exposure task "exposeExperts" should be in group "generate"

  Scenario: Expose experts generates manifest with domain info
    When I expose experts with anonymization disabled
    Then the manifest file exists
    And the manifest contains domain "kotlin"
    And the manifest contains domain "docs"
    And the manifest contains model "gpt-oss:120b-cloud"
    And the manifest is valid JSON

  Scenario: Anonymized endpoints hide baseUrl
    When I expose experts with anonymization enabled
    Then the manifest file exists
    And the manifest contains "***anonymized***"
    And the manifest does not contain "http://localhost:11437"

  Scenario: Filtered domains only expose selected experts
    When I expose experts with domains "kotlin" and anonymization disabled
    Then the manifest file exists
    And the manifest contains domain "kotlin"
    And the manifest does not contain domain "docs"

  Scenario: Manifest is consumable by slider/plantuml/bakery
    When I expose experts with anonymization disabled
    Then the manifest contains "version"
    And the manifest contains "generatedAt"
    And the manifest contains "experts"
    And the manifest contains "modelName"
    And the manifest contains "timeoutSeconds"
