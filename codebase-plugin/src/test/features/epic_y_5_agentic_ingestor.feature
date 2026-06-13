@epic_y_5
Feature: Agentic Ingestor — Pipeline Complet d'Ingestion
  As a codebase-gradle developer
  I want to ingest .adoc files through the full pipeline (chunk → ontologize → store → compile)
  So that agentic literature becomes executable artifacts in the database

  @ingest
  Scenario: Ingest a single AGENT.adoc file with rules and procedures
    Given the agentic schema is initialized for ingestion
    And an AGENT.adoc file with rules and procedures
    When I ingest the files
    Then the ingestion report shows chunks added > 0
    And the ingestion report shows artifacts compiled > 0
    And the database contains the ingested chunks

  @idempotent
  Scenario: Re-ingesting the same file should skip all chunks
    Given the agentic schema is initialized for ingestion
    And an AGENT.adoc file with rules and procedures
    When I ingest the files
    And I ingest the same files again
    Then the ingestion report shows chunks added = 0
    And the ingestion report shows chunks skipped > 0

  @modification
  Scenario: Modifying a file should detect changed chunks
    Given the agentic schema is initialized for ingestion
    And an AGENT.adoc file with rules and procedures
    When I ingest the files
    And I modify the AGENT.adoc content and re-ingest
    Then the ingestion report shows chunks modified > 0

  @batch
  Scenario: Ingest multiple files in one batch
    Given the agentic schema is initialized for ingestion
    And an AGENT.adoc file with rules and procedures
    And an INDEX.adoc file with metadata
    When I ingest the files
    Then the ingestion report shows chunks added > 0
    And the ingestion report shows files scanned = 2
    And the database contains chunks from multiple files

  @taxonomy
  Scenario: Ingest TAXONOMIE_WORKSPACE and verify all taxonomy sections
    Given the agentic schema is initialized for ingestion
    And a TAXONOMIE_WORKSPACE.adoc file with all taxonomy sections
    When I ingest the files
    Then the ingestion report shows chunks added > 0
    And the ingestion report shows artifacts compiled > 0
    And the database contains chunks with taxonomy sections PRINCIPES, TAXONOMIE, FORMAT_PIVOT, CONVENTION_OVER_CONFIGURATION, CONFIG_DOMAINE, MAPPING, ROADMAP_IMPLEMENTATION, DEPENDANCES, ORDRE_ATTAQUE, EXEMPLES_STDOUT, and CONCLUSION
