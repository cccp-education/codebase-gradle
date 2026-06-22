@epic_v_9_8
Feature: Governance ingestion report — GovernanceOntologizer integrated into IngestGovernanceTask
  As a codebase-gradle developer
  I want the ingestion task to enrich IngestionReport with GovernanceSection counts
  So that I can track how many chunks belong to rules, state, backlog, history, coverage, and mission

  Scenario: Ingesting AGENT and INDEX adoc reports RULES_ABSOLUES and ETAT_EPICS sections
    Given a temporary project with governance files
      | sourceFile  | content                                              |
      | AGENT.adoc  | = Agent\n\n* NE DOIT JAMAIS leak de secrets\n          |
      | INDEX.adoc  | = Index\n\n== EPIC Y\nAgentic Literature Compiler en cours.\n |
    When I run the ingestGovernance task on the project
    Then the ingestion report has sections added
      | section          |
      | RULES_ABSOLUES   |
      | ETAT_EPICS       |
    And the ingestion report has sections total
      | section          |
      | RULES_ABSOLUES   |
      | ETAT_EPICS       |

  Scenario: Ingestion report JSON includes sectionsAdded and sectionsTotal
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task with output file "ingestion-report.json"
    Then the output JSON contains "sectionsAdded"
    And the output JSON contains "sectionsTotal"
    And the output JSON contains "RULES_ABSOLUES"

  Scenario: Unknown governance file contributes to UNKNOWN section
    Given a temporary project with governance file "README.adoc"
      """
      = README

      This is a normal readme file.
      """
    When I run the ingestGovernance task on the project
    Then the ingestion report section total is "UNKNOWN" with count greater than 0
