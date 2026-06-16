@epic7 @wip
Feature: EPIC 7 - Wiring LangChain4j + pgvector Experts

  As a workspace developer
  I want a dispatcher to decompose complex tasks and route them to domain experts
  So that specialized LLM agents handle subtasks with domain-specific knowledge

  Background:
    Given an expert registry with "kotlin" and "docs" domains
    And a dispatcher agent is initialized with the registry

  Scenario: Dispatch a single-domain task to the correct expert
    When I dispatch the task "Write a Gradle plugin" with domain hints "kotlin"
    Then the dispatcher decomposes into at least 1 subtasks
    And all subtasks are assigned to the "kotlin" domain
    And all expert calls succeed
    And the synthesis output is not empty

  Scenario: Dispatch a multi-domain task to multiple experts
    When I dispatch the task "Build a plugin with documentation" with domain hints "kotlin,docs"
    Then the dispatcher decomposes into at least 2 subtasks
    And subtasks are assigned to both "kotlin" and "docs" domains
    And all expert calls succeed

  Scenario: Dispatch to an unregistered domain returns error
    When I dispatch the task "Analyze data" with domain hints "unknown"
    Then at least 1 expert call fails
    And the failed call error contains "No expert registered"

  Scenario: Pipeline anonymizes PII before dispatching
    Given a pipeline with anonymization is initialized
    When I execute the pipeline with prompt "Use API key: sk-12345"
    Then the anonymized prompt does not contain "sk-12345"
    And the anonymized prompt contains "***"
    And the dispatcher receives the anonymized prompt

  Scenario: Pipeline persists expert calls to database
    Given a pipeline with persistence is initialized
    When I execute the pipeline with prompt "Write documentation"
    Then at least 1 expert call is persisted
    And the persisted calls can be retrieved by task ID
