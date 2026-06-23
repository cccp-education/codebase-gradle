@epic_v_9_10
Feature: Chunk validation report — errors exposed by file, line and type
  As a codebase-gradle developer
  I want validation errors to be exposed in a structured report
  So that I can triage governance issues by source file, line range and error category

  Scenario: Validation errors include source file, line range and error type
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    And the ingestGovernance task uses a validator that rejects all chunks as MISSING_CONTENT
    When I run the ingestGovernance task with output file "ingestion-report.json"
    Then the output JSON contains "errorType"
    And the output JSON contains "lineStart"
    And the output JSON contains "lineEnd"
    And the output JSON contains "validationErrorsByType"

  Scenario: Validation errors are grouped by error type in the summary
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    And the ingestGovernance task uses a validator that rejects all chunks as MISSING_CONTENT
    When I run the ingestGovernance task with output file "ingestion-report.json"
    Then the output JSON matches the validation error type summary for "MISSING_CONTENT"

  Scenario: Valid chunks produce an empty validation error summary
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task with output file "ingestion-report.json"
    Then the output JSON contains "\"validationErrorsByType\": {}"
    And the output JSON contains "\"validationErrors\": []"
