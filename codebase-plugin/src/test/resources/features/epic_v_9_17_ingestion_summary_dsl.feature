Feature: EPIC V-9.17 Ingestion Summary DSL

  As a workspace maintainer
  I want to configure governance ingestion via `codebaseGovernance { }` DSL or a CLI property
  So that strict validation and output format are explicit and discoverable

  @epic_v_9_17
  Scenario: DSL strictValidation false is the default
    Given a Gradle project with AGENT.adoc containing "* NE DOIT JAMAIS leak secrets"
    When the plugin is applied with no governance configuration
    Then the ingestGovernance task has strictValidation disabled

  @epic_v_9_17
  Scenario: DSL strictValidation can be enabled via extension
    Given a Gradle project with AGENT.adoc containing "* NE DOIT JAMAIS leak secrets"
    When the plugin is applied with "codebaseGovernance { strictValidation.set(true) }"
    Then the ingestGovernance task has strictValidation enabled

  @epic_v_9_17
  Scenario: CLI property overrides extension default
    Given a Gradle project with AGENT.adoc containing "* NE DOIT JAMAIS leak secrets"
    And the gradle property "codebase.governance.strictValidation" is set to "true"
    When the plugin is applied with "codebaseGovernance { strictValidation.set(false) }"
    Then the ingestGovernance task has strictValidation enabled
