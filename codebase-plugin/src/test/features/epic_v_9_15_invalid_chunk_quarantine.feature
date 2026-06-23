@epic_v_9_15
Feature: Invalid chunk quarantine
  As a codebase-gradle developer
  I want invalid chunks detected during governance ingestion to be quarantined
  So that I can diagnose validation failures without losing the original content

  @ingest_task
  Scenario: Invalid chunks are quarantined in IngestionReport
    Given a quarantine test project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    And a chunk validator that rejects every chunk with checksum mismatch
    When I run IngestGovernanceTask for quarantine on the project
    Then the ingestion report should contain quarantined chunks
    And each quarantined chunk should have at least one error

  @ingest_task @json_report
  Scenario: JSON report exposes invalid chunks with metadata
    Given a quarantine test project with file "AGENT.adoc" containing
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    And a chunk validator that rejects every chunk with checksum mismatch
    When I run IngestGovernanceTask for quarantine with output file "quarantine-report.json"
    Then the output file should contain "invalidChunks"
    And the output file should contain "quarantinedAt"
    And the output file should contain "CHECKSUM_MISMATCH"
