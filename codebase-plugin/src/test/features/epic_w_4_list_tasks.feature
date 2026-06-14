@epic_w_4
Feature: List Tasks — LLM discovers available Gradle tasks
  As a codebase-gradle vibecoding agent
  I want to query available Gradle tasks with descriptions and options
  So that I can select the right task for the user's intention

  @catalog_all
  Scenario: List all available tasks without filter
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with no arguments
    Then the result contains task "gradle_build"
    And the result contains task "gradle_test"
    And the result contains task "gradle_publish"

  @catalog_group
  Scenario: Filter tasks by group
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with group "build"
    Then the result contains task "gradle_build"
    And the result does NOT contain task "gradle_test"

  @catalog_keyword
  Scenario: Search tasks by keyword in name
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with keyword "test"
    Then the result contains task "gradle_test"
    And the result does NOT contain task "gradle_build"

  @catalog_keyword_description
  Scenario: Search tasks by keyword in description
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with keyword "compiles"
    Then the result contains task "gradle_build"
    And the result does NOT contain task "gradle_publish"
    And the result does NOT contain task "gradle_test"

  @catalog_options
  Scenario: List tasks shows options when available
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with no arguments
    Then the result includes option "--repository" for task "gradle_publish"
    And the result includes option "--dryRun" for task "gradle_publish"

  @catalog_empty
  Scenario: No match returns empty message
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with keyword "nonexistent"
    Then the result shows "No tasks found"

  @catalog_dry_run
  Scenario: Dry-run mode returns DRY RUN prefix
    Given a ToolRegistry with task schemas registered
    When I call list_tasks with dryRun
    Then the result starts with "DRY RUN"
