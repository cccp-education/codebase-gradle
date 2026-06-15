@epic_sp_6
Feature: Session Protocol SP-6 — LiveContextInjector injection contexte conversation active
  As a thin client (opencode terminal)
  I want the LLM to receive live conversation context (history, tool calls, static context)
  So that the agent makes informed decisions based on the full session state

  @sp6_live_context_basic
  Scenario: Live context is injected into the LLM prompt during vibecoding
    Given an SP-6 SessionProtocolTask with live context injector enabled
    When I SP-6 execute action "create" with prompt "Add dark mode toggle"
    Then the SP-6 LLM prompt contains live context section
    And the SP-6 LLM prompt contains the intention "Add dark mode toggle"
    And the SP-6 LLM prompt contains iteration metadata

  @sp6_live_context_static
  Scenario: Static context from contextFile is injected into the LLM prompt
    Given an SP-6 SessionProtocolTask with live context injector enabled
    And an SP-6 context file with eager rules and backlog items
    When I SP-6 execute action "create" with prompt "Refactor authentication" and context file
    Then the SP-6 LLM prompt contains static context section
    And the SP-6 LLM prompt contains the eager rules from context file
    And the SP-6 LLM prompt contains the backlog items from context file

  @sp6_live_context_iteration
  Scenario: Live context is injected on subsequent iterations with updated state
    Given an SP-6 SessionProtocolTask with live context injector enabled
    When I SP-6 execute action "create" with prompt "Build the project" and maxActions 3
    Then the SP-6 LLM prompt contains live context section
    And the SP-6 LLM prompt contains iteration metadata
