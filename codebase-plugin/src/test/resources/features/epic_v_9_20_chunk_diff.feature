Feature: EPIC V-9.20 Chunk diff incremental between sessions

  As a workspace maintainer
  I want ingestGovernance to detect chunk-level content changes between sessions
  So that I can track exactly which chunks were added, modified, removed, or unchanged

  @epic_v_9_20
  Scenario: First chunk incremental run produces report with all added chunks
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in chunk incremental mode
    Then the chunk incremental report is not null
    And the chunk incremental report has chunks added greater than 0
    And the chunk incremental report has chunks modified equal to 0
    And the chunk incremental report has chunks removed equal to 0
    And the chunk incremental report has chunks unchanged equal to 0

  @epic_v_9_20
  Scenario: Second chunk incremental run with no changes lists all chunks as unchanged
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in chunk incremental mode
    Then the chunk incremental report has chunks added greater than 0
    When I run the ingestGovernance task in chunk incremental mode
    Then the chunk incremental report has chunks added equal to 0
    And the chunk incremental report has chunks modified equal to 0
    And the chunk incremental report has chunks removed equal to 0
    And the chunk incremental report has chunks unchanged greater than 0

  @epic_v_9_20
  Scenario: Chunk incremental detects new chunk when content is appended to file
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in chunk incremental mode
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      * DOIT valider les tests avant fin de session
      """
    When I run the ingestGovernance task in chunk incremental mode
    Then the chunk incremental report has chunks added greater than 0
    And the chunk incremental report has chunks modified equal to 0

  @epic_v_9_20
  Scenario: Chunk incremental report is exposed in output JSON
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in chunk incremental mode with output file "report.json"
    Then the output JSON contains "chunkIncremental"
    And the output JSON contains "chunksAdded"
    And the output JSON contains "chunksModified"
    And the output JSON contains "chunksRemoved"
    And the output JSON contains "chunksUnchanged"

  @epic_v_9_20
  Scenario: Chunk incremental is independent of file incremental mode
    Given a temporary project with governance file "AGENT.adoc"
      """
      = Agent

      * NE DOIT JAMAIS leak de secrets
      """
    When I run the ingestGovernance task in file and chunk incremental mode
    Then the incremental report is not null
    And the chunk incremental report is not null