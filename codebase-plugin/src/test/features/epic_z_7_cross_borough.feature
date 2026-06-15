@epic_z @epic_z_7
Feature: EPIC Z-7 — Validation Cross-Borough Autofocus Bout-en-Bout
  As a codebase-gradle developer
  I want to validate the full autofocus pipeline end-to-end
  from intention classification through zoomed execution to success
  with cross-borough context (codex N2, planner N2, runner N3)
  So that the zoomedContext is correctly transmitted in the entire chain

  Background:
    Given a VibecodingGraph is initialized with Gemini fake chat model
    And a composite context with cross-borough references is prepared

  Scenario: Full pipeline — intention classify zoom execute success popFocus
    When I execute vibecoding with a plan that succeeds and intention "fix compilation error in VibecodingGraph.kt:42"
    Then the vibecoding result state is finished
    And the vibecoding autofocus stack is empty after success pop
    And the vibecoding zoomed context was used during execution

  Scenario: Cross-borough context is preserved in zoomed context
    When I execute vibecoding with a plan that succeeds and intention "refactor cross-borough DAG N1→N2→N3"
    Then the vibecoding result state is finished
    And the vibecoding autofocus stack is empty after success pop
    And the vibecoding zoomed context graphify section contains "codebase"
    And the vibecoding zoomed context graphify section contains "codex"
    And the vibecoding zoomed context graphify section contains "planner"
    And the vibecoding zoomed context graphify section contains "runner"

  Scenario: Error recovery — zoom-in IMPLEMENTATION retry success popFocus back to original
    When I execute vibecoding with a plan that fails once then succeeds and intention "refactor module structure"
    Then the vibecoding result state is finished
    And the vibecoding autofocus stack is empty after success pop
    And the vibecoding error was recovered

  Scenario: Multi-level zoom cycle — BIG_PICTURE error IMPLEMENTATION success pop BIG_PICTURE
    When I execute vibecoding with a plan that fails once then succeeds and intention "plan EPIC Z roadmap for codebase-gradle"
    Then the vibecoding result state is finished
    And the vibecoding autofocus stack is empty after success pop
    And the vibecoding error was recovered
