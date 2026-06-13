@epic_y_6
Feature: External Agentic Literature Import
  As a codebase-gradle developer
  I want to import external agentic literature (Copilot, Cursor, Claude, Gemini)
  So that external rules and prompts become executable artifacts in the agentic pipeline

  @copilot
  Scenario: Import Copilot rules and detect prohibitions
    Given the agentic schema is initialized for ingestion
    And a Copilot rules file with INTERDICTION statements
    When I import the external content as "copilot"
    Then the ingestion report shows chunks added > 0
    And the ingestion report shows artifacts compiled > 0
    And the database contains chunks from external source "copilot"

  @cursor
  Scenario: Import Cursor rules with YAML frontmatter
    Given the agentic schema is initialized for ingestion
    And a Cursor rules file with YAML frontmatter
    When I import the external content as "cursor"
    Then the ingestion report shows chunks added > 0
    And the ingestion report shows artifacts compiled > 0
    And the database contains chunks from external source "cursor"

  @claude
  Scenario: Import Claude agent system prompt with constraints
    Given the agentic schema is initialized for ingestion
    And a Claude agent system prompt with constraints
    When I import the external content as "claude"
    Then the ingestion report shows chunks added > 0
    And the database contains CONSTRAINT chunks

  @batch
  Scenario: Import multiple external sources in sequence
    Given the agentic schema is initialized for ingestion
    When I import Copilot rules as "copilot"
    And I import Cursor rules as "cursor"
    Then the database contains chunks from external source "copilot"
    And the database contains chunks from external source "cursor"
    And the database contains chunks from multiple files

  @edge
  Scenario: Import empty external content should produce zero report
    Given the agentic schema is initialized for ingestion
    And empty external content
    When I import the external content as "copilot"
    Then the ingestion report shows chunks added = 0
    And the ingestion report shows artifacts compiled = 0
