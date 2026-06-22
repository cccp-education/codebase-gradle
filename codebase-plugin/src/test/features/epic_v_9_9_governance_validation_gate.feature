@epic_v_9_9
Feature: Governance ingestion validation gate — ChunkValidator wired into AgenticIngestor
  As a codebase-gradle developer
  I want the ingestion pipeline to validate every chunk before storage
  So that invalid chunks are rejected and reported instead of polluting the repository

  Scenario: Ingesting valid governance files reports zero invalid chunks
    Given a temporary project with governance files
      | sourceFile  | content                                              |
      | AGENT.adoc  | = Agent\n\n* NE DOIT JAMAIS leak de secrets\n          |
      | INDEX.adoc  | = Index\n\n== EPIC Y\nAgentic Literature Compiler en cours.\n |
    When I run the ingestGovernance task on the project
    Then the ingestion report has chunks invalid equal to 0
    And the ingestion report has validation errors count equal to 0

  Scenario: Ingestion report JSON includes validation counters
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task with output file "ingestion-report.json"
    Then the output JSON contains "chunksInvalid"
    And the output JSON contains "validationErrors"
    And the output JSON contains "RULES_ABSOLUES"
