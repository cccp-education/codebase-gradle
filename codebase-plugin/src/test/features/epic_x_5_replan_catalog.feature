@epic_x_5
Feature: Task Catalog in Replan Prompt — LLM uses task catalog to replan
  As a vibecoding agent
  I want the task catalog included in replan prompts
  So that I can suggest alternative Gradle tasks when recovering from errors

  @replan_with_catalog
  Scenario: Replan prompt includes task catalog when schemas are available
    Given a VibecodingGraph with task schemas
    And a VibecodingState with error "BUILD FAILED" and retryCount 1 and maxRetries 3
    When the replan prompt is built
    Then the prompt contains "Available Gradle tasks"
    And the prompt contains at least one task name

  @replan_without_catalog
  Scenario: Replan prompt does not include task catalog when no schemas
    Given a VibecodingGraph without task schemas
    And a VibecodingState with error "BUILD FAILED" and retryCount 1 and maxRetries 3
    When the replan prompt is built
    Then the prompt does not contain "Available Gradle tasks"

  @replan_catalog_filtered
  Scenario: Replan prompt includes task catalog with multiple tasks
    Given a VibecodingGraph with 3 task schemas
    And a VibecodingState with error "BUILD FAILED" and retryCount 1 and maxRetries 3
    When the replan prompt is built
    Then the prompt contains all 3 task names
