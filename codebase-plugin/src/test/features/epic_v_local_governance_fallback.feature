@epic_v_local
Feature: Vibecoding Local — Governance Context Auto-Loading
  As an opencode thin client
  I want the SessionProtocolTask to auto-load governance EAGER files when no contextFile is provided
  So that AGENT.adoc, INDEX.adoc and PROMPT_REPRISE.adoc are injected without manual wiring

  @governance_fallback_loads_agent
  Scenario: AGENT.adoc is auto-loaded into AgentContext eagerRules
    Given a project workspace with AGENT.adoc containing "Rule 7: never commit without permission"
    And a governance fallback task configured with FakeLlmProvider
    When I send prompt "Continue work" without contextFile
    Then the auto-loaded AgentContext eagerRules contains "never commit without permission"
    And the governance response status is COMPLETED

  @governance_fallback_loads_index
  Scenario: INDEX.adoc from .agents is auto-loaded into AgentContext eagerRules
    Given a project workspace with a governance INDEX file containing "EPIC V-LOCAL en cours"
    And a governance fallback task configured with FakeLlmProvider
    When I send prompt "Resume session" without contextFile
    Then the auto-loaded AgentContext eagerRules contains "EPIC V-LOCAL en cours"

  @governance_fallback_loads_backlog
  Scenario: BACKLOG.adoc items are extracted into AgentContext backlogItems
    Given a project workspace with BACKLOG.adoc containing checkbox items
    And a governance fallback task configured with FakeLlmProvider
    When I send prompt "What is next" without contextFile
    Then the auto-loaded AgentContext backlogItems contains "Open item V-LOCAL-4"
    And the auto-loaded AgentContext backlogItems contains "Done item V-LOCAL-2"

  @governance_fallback_empty_workspace
  Scenario: Empty workspace produces empty AgentContext but still responds
    Given an empty project workspace
    And a governance fallback task configured with FakeLlmProvider
    When I send prompt "Cold start" without contextFile
    Then the auto-loaded AgentContext eagerRules is empty
    And the governance response status is COMPLETED

  @governance_fallback_subproject
  Scenario: Governance files in first-level subproject are auto-loaded
    Given a multi-module project with AGENT.adoc in subproject "codebase-plugin"
    And a governance fallback task configured with FakeLlmProvider
    When I send prompt "Cross-module prompt" without contextFile
    Then the auto-loaded AgentContext eagerRules contains "Subproject agent rules"