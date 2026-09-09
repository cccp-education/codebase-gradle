@corpus-ingest
Feature: CB-FPA-RAG — corpus ingestion into the RAG socle
  As a codebase-gradle maintainer
  I want the codex chunks corpus loaded, doubt-annotated and ingested via ingestWithDoubt
  So that the FPA corpus becomes queryable augmented context (GBL-003.4 critical path)

  Background:
    Given a corpus ingest world is initialized

  Scenario: Codex chunks json loads into typed DocumentChunks
    Given a codex chunks file with 3 chunks, 1 carrying the ILLISIBLE marker
    When the corpus chunks are loaded
    Then 3 typed chunks are loaded
    And every loaded chunk keeps its source document "book"

  Scenario: Doubt policy flags ILLISIBLE chunks and keeps clean chunks confident
    Given a codex chunks file with clean content "referentiel de competences" and doubtful content "page [ILLISIBLE] scan"
    When the corpus chunks are marked for ingestion
    Then 1 chunk is doubtful with confidence 0.0
    And 1 chunk is confident with full confidence

  Scenario: Ingestion contracts the doubt-aware store call
    Given 4 codex chunks marked for ingestion with 2 doubtful
    When the corpus ingestion is executed against the fake store
    Then the store receives 1 document
    And the store records 4 chunks with 2 doubtful

  Scenario: Composite context Docs channel exposes the ingested corpus metadata
    Given a rag store stub returning a doubtful result and a confident result for query "referentiel competences"
    When the docs context is loaded for the corpus query "referentiel competences"
    Then the docs section annotates doubtful chunks
    And the docs section keeps confident chunks unannotated