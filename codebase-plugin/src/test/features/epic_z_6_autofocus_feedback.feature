@epic_z @epic_z_6
Feature: EPIC Z-6 — Autofocus Feedback Loop Integration
  As a codebase-gradle developer
  I want to validate that when verifyResult FAILED, the autofocus zooms to IMPLEMENTATION
  and the replan prompt includes zoomed context for surgical error recovery
  So that the agent can retry with precise, focused context instead of the full workspace

  Background:
    Given a VibecodingGraph is initialized with Gemini fake chat model
    And a composite context with 4 channels is prepared

  Scenario: Error triggers autofocus zoom-in to IMPLEMENTATION
    When I execute vibecoding with a plan that will fail and maxRetries 2
    Then the vibecoding focus level is "IMPLEMENTATION"
    And the vibecoding zoomed context is not null
    And the vibecoding zoomed context eager section is empty
    And the vibecoding zoomed context graphify section is empty
    And the vibecoding zoomed context rag section is not empty

  Scenario: Replan prompt includes zoomed IMPLEMENTATION context
    When I execute vibecoding with a plan that will fail and maxRetries 2
    Then the vibecoding replan prompt contains "ZOOMED CONTEXT"
    And the vibecoding replan prompt contains "IMPLEMENTATION"

  Scenario: Autofocus stack is populated after error zoom-in
    When I execute vibecoding with a plan that will fail and maxRetries 2
    Then the vibecoding autofocus stack is not empty
    And the vibecoding autofocus stack size is at least 2

  Scenario: Error recovery without composite context does not crash
    When I execute vibecoding with a plan that will fail and maxRetries 1 without composite context
    Then the vibecoding result state is finished or final
