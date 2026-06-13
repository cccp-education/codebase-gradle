@epic_y_4
Feature: Agentic Compiler — Transformation Chunk Ontologise → Artefact Executable
  As a codebase-gradle developer
  I want to compile ontologized chunks into executable artifacts
  So that rules become pre-hooks, procedures become Gradle tasks, and constraints become validations

  @rule
  Scenario: Compile a RULE INTERDIRE chunk into a PRE_HOOK
    Given a chunk of type "RULE" with verb "INTERDIRE" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "PRE_HOOK"
    And the compiled artifact target hint is "codebase"
    And the compiled artifact confidence is at least 0.8
    And the compiled artifact description contains "INTERDICTION"

  @rule
  Scenario: Compile a RULE with no verb into a CI_GATE
    Given a chunk of type "RULE" with verb "VALIDER" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "CI_GATE"

  @procedure
  Scenario: Compile a PROCEDURE VALIDER into a VALIDATION
    Given a chunk of type "PROCEDURE" with verb "VALIDER" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "VALIDATION"

  @procedure
  Scenario: Compile a PROCEDURE GENERER into a GRADLE_TASK
    Given a chunk of type "PROCEDURE" with verb "GENERER" and domain "training"
    When I compile the chunk
    Then the compiled artifact type is "GRADLE_TASK"

  @constraint
  Scenario: Compile a CONSTRAINT VALIDER into a CONSTRAINT_CHECK
    Given a chunk of type "CONSTRAINT" with verb "VALIDER" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "CONSTRAINT_CHECK"

  @concept
  Scenario: Compile a CONCEPT GENERER into a METADATA
    Given a chunk of type "CONCEPT" with verb "GENERER" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "METADATA"

  @concept
  Scenario: Compile a CONCEPT INTERDIRE into a PROMPT_TEMPLATE
    Given a chunk of type "CONCEPT" with verb "INTERDIRE" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "PROMPT_TEMPLATE"

  @metadata
  Scenario: Compile a METADATA chunk into a METADATA artifact
    Given a chunk of type "METADATA" with verb "VALIDER" and domain "codebase"
    When I compile the chunk
    Then the compiled artifact type is "METADATA"

  @batch
  Scenario: Compile all chunks from TAXONOMIE_WORKSPACE
    Given a TAXONOMIE_WORKSPACE document is chunked and ontologized for compilation
    When I compile all chunks
    Then at least 11 artifacts are compiled
    And the compiled artifacts include types "METADATA" and "PROMPT_TEMPLATE"
