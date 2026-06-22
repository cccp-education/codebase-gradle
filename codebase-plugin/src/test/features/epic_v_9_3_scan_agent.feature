@epic_v_9_3
Feature: ScanAgent — Scanner récursif des fichiers .adoc du dossier .agents/
  As a codebase-gradle developer
  I want to scan the .agents/ directory recursively to discover all governance .adoc files
  So that the ingestion pipeline no longer relies on a hardcoded list of paths

  @empty_directory
  Scenario: Scanning an empty directory returns no files
    Given an empty workspace directory
    When I scan the directory with ScanAgent
    Then the scanned files list is empty

  @root_files
  Scenario: Scanning finds adoc files at the workspace root
    Given a workspace directory with the following adoc files at root
      | filename      | content            |
      | AGENT.adoc    | = Agent\n* Rule    |
      | BACKLOG.adoc  | = Backlog\n* Item  |
    When I scan the directory with ScanAgent
    Then the scanned files count is 2
    And the scanned files contain "AGENT.adoc"
    And the scanned files contain "BACKLOG.adoc"

  @recursive_agents
  Scenario: Scanning recursively discovers all adoc files in .agents/
    Given a workspace directory with a nested .agents structure
    When I scan the directory with ScanAgent
    Then the scanned files count is 4
    And the scanned files contain ".agents/INDEX.adoc"
    And the scanned files contain ".agents/SESSIONS_HISTORY.adoc"
    And the scanned files contain ".agents/sessions/001-test.adoc"

  @subproject
  Scenario: Scanning discovers adoc files in subproject directories
    Given a workspace directory with a subproject containing .agents files
    When I scan the directory with ScanAgent
    Then the scanned files count is 2
    And the scanned files contain "my-plugin/AGENT.adoc"
    And the scanned files contain "my-plugin/.agents/INDEX.adoc"

  @ignored_dirs
  Scenario: Scanning ignores build and git directories
    Given a workspace directory with adoc files in build and .git directories
    When I scan the directory with ScanAgent
    Then the scanned files count is 1
    And the scanned files do not contain "build"
    And the scanned files do not contain ".git"

  @ingest_integration
  Scenario: IngestGovernanceTask uses ScanAgent to discover files dynamically
    Given a workspace directory with adoc files at root and in .agents
    When IngestGovernanceTask ingests the workspace
    Then the ingestion report files scanned is greater than 0
    And the ingestion report chunks added is greater than 0