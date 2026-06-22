@epic_v_9_7
Feature: GovernanceOntologizer — Classify governance files into project-specific sections
  As a codebase-gradle developer
  I want to classify chunks by their governance source file
  So that the ingestion pipeline can route AGENT rules, INDEX state, backlog items, history, coverage, and mission separately

  Scenario: AGENT.adoc chunk is classified as RULES_ABSOLUES
    Given a chunk from source file "AGENT.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "RULES_ABSOLUES"

  Scenario: INDEX.adoc chunk is classified as ETAT_EPICS
    Given a chunk from source file ".agents/INDEX.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "ETAT_EPICS"

  Scenario: BACKLOG.adoc chunk is classified as BACKLOG_ITEMS
    Given a chunk from source file "BACKLOG.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "BACKLOG_ITEMS"

  Scenario: SESSIONS_HISTORY.adoc chunk is classified as HISTORIQUE
    Given a chunk from source file ".agents/SESSIONS_HISTORY.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "HISTORIQUE"

  Scenario: TEST_COVERAGE_ANALYSIS.adoc chunk is classified as COVERAGE
    Given a chunk from source file ".agents/TEST_COVERAGE_ANALYSIS.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "COVERAGE"

  Scenario: PROMPT_REPRISE.adoc chunk is classified as MISSION
    Given a chunk from source file "PROMPT_REPRISE.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "MISSION"

  Scenario: Unknown adoc is classified as UNKNOWN
    Given a chunk from source file "README.adoc"
    When I classify the chunk with GovernanceOntologizer
    Then the governance section is "UNKNOWN"
