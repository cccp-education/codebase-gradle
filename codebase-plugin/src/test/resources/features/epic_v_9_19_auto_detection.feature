Feature: EPIC V-9.19 Auto-detection of new governance files

  As a workspace maintainer
  I want ingestGovernance to detect new and modified governance files incrementally
  So that re-ingestion only processes changed files instead of the whole project

  @epic_v_9_19
  Scenario: First incremental run ingests all governance files
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    Given a temporary project with governance file "BACKLOG.adoc"
      """
      = Backlog

      * [ ] Open item
      """
    When I run the ingestGovernance task in incremental mode
    Then the incremental report lists "AGENT.adoc" as added
    And the incremental report lists "BACKLOG.adoc" as added
    And the ingestion report has files scanned greater than 0

  @epic_v_9_19
  Scenario: Second incremental run with no changes skips all files
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in incremental mode
    Then the incremental report lists "AGENT.adoc" as added
    When I run the ingestGovernance task in incremental mode
    Then the incremental report lists "AGENT.adoc" as unchanged
    And the ingestion report has files scanned equal to 0

  @epic_v_9_19
  Scenario: Incremental run detects modified file and reingests only it
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    Given a temporary project with governance file "BACKLOG.adoc"
      """
      = Backlog

      * [ ] Open item
      """
    When I run the ingestGovernance task in incremental mode
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      * DOIT valider les tests avant fin de session
      """
    When I run the ingestGovernance task in incremental mode
    Then the incremental report lists "AGENT.adoc" as modified
    And the incremental report lists "BACKLOG.adoc" as unchanged
    And the ingestion report has files scanned equal to 1

  @epic_v_9_19
  Scenario: Incremental run detects newly added file
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in incremental mode
    Given a temporary project with governance file "INDEX.adoc"
      """
      = Index

      == EPIC Y
      En cours.
      """
    When I run the ingestGovernance task in incremental mode
    Then the incremental report lists "INDEX.adoc" as added
    And the incremental report lists "AGENT.adoc" as unchanged

  @epic_v_9_19
  Scenario: Incremental report is exposed in output JSON
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in incremental mode with output file "report.json"
    Then the output JSON contains "incremental"
    And the output JSON contains "added"