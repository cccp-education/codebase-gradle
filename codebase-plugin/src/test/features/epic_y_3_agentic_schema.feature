@epic_y_3
Feature: Agentic Schema — Persistance des Chunks Ontologises
  As a codebase-gradle developer
  I want to persist ontologized chunks in PostgreSQL with pgvector embeddings
  So that I can query chunks by domain, verb, DAG level, taxonomy section, and semantic similarity

  Background:
    Given the agentic schema is initialized

  @insert
  Scenario: Insert and retrieve a single ontologized chunk
    When I insert an ontologized chunk with id "chunk-001"
    Then the chunk is persisted successfully
    When I retrieve the chunk with id "chunk-001"
    Then the retrieved chunk has id "chunk-001"
    And the retrieved chunk has verb "INTERDIRE"
    And the retrieved chunk has domain "codebase"
    And the retrieved chunk has DAG level "N1"
    And the retrieved chunk has taxonomy section "PRINCIPES"
    And the retrieved chunk has ontology confidence 0.9

  @list
  Scenario: List all chunks after batch insert from TAXONOMIE_WORKSPACE
    Given a TAXONOMIE_WORKSPACE document is chunked and ontologized
    When I list all chunks
    Then the listed chunks contain at least 11 items
    And the listed chunks contain taxonomy sections PRINCIPES, TAXONOMIE, FORMAT_PIVOT, CONVENTION_OVER_CONFIGURATION, CONFIG_DOMAINE, MAPPING, ROADMAP_IMPLEMENTATION, DEPENDANCES, ORDRE_ATTAQUE, EXEMPLES_STDOUT, and CONCLUSION

  @filter
  Scenario: Filter chunks by domain after inserting specific chunks
    When I insert an ontologized chunk with id "dom-1" and domain "codebase"
    And I insert an ontologized chunk with id "dom-2" and domain "planner"
    And I insert an ontologized chunk with id "dom-3" and domain "codebase"
    When I list chunks by domain "codebase"
    Then the listed chunks contain at least 2 items
    And all listed chunks have domain "codebase"

  @filter
  Scenario: Filter chunks by verb after inserting specific chunks
    When I insert an ontologized chunk with id "verb-1" and verb "GENERER"
    And I insert an ontologized chunk with id "verb-2" and verb "INTERDIRE"
    And I insert an ontologized chunk with id "verb-3" and verb "GENERER"
    When I list chunks by verb "GENERER"
    Then the listed chunks contain at least 2 items
    And all listed chunks have verb "GENERER"

  @filter
  Scenario: Filter chunks by DAG level after inserting specific chunks
    When I insert an ontologized chunk with id "dag-1" and dagLevel "N1"
    And I insert an ontologized chunk with id "dag-2" and dagLevel "N2"
    And I insert an ontologized chunk with id "dag-3" and dagLevel "N1"
    When I list chunks by DAG level "N1"
    Then the listed chunks contain at least 2 items
    And all listed chunks have DAG level "N1"

  @filter
  Scenario: Filter chunks by taxonomy section after inserting specific chunks
    When I insert an ontologized chunk with id "tax-1" and taxonomySection "PRINCIPES"
    And I insert an ontologized chunk with id "tax-2" and taxonomySection "TAXONOMIE"
    And I insert an ontologized chunk with id "tax-3" and taxonomySection "PRINCIPES"
    When I list chunks by taxonomy section "PRINCIPES"
    Then the listed chunks contain at least 2 items
    And all listed chunks have taxonomy section "PRINCIPES"

  @count
  Scenario: Count chunks after batch insert
    Given a TAXONOMIE_WORKSPACE document is chunked and ontologized
    When I count chunks
    Then the chunk count is at least 11

  @relations
  Scenario: Insert and retrieve chunk relations
    When I insert an ontologized chunk with id "rel-src"
    And I insert an ontologized chunk with id "rel-tgt"
    And I insert a relation "DEPENDS_ON" from "rel-src" to "rel-tgt" with confidence 0.8
    Then the relation is created with a valid id
    When I get relations for chunk "rel-src"
    Then the relations list contains 1 items
    And the relations include type "DEPENDS_ON"

  @relations
  Scenario: Multiple relation types between same chunks
    When I insert an ontologized chunk with id "multi-src"
    And I insert an ontologized chunk with id "multi-tgt"
    And I insert a relation "DEPENDS_ON" from "multi-src" to "multi-tgt" with confidence 0.8
    And I insert a relation "REFINES" from "multi-src" to "multi-tgt" with confidence 0.6
    When I get relations for chunk "multi-src"
    Then the relations list contains 2 items
    And the relations include type "DEPENDS_ON"
    And the relations include type "REFINES"

  @relations
  Scenario: Count relations
    When I insert an ontologized chunk with id "count-src"
    And I insert an ontologized chunk with id "count-tgt1"
    And I insert an ontologized chunk with id "count-tgt2"
    And I insert a relation "ENFORCES" from "count-src" to "count-tgt1" with confidence 0.7
    And I insert a relation "DEPENDS_ON" from "count-src" to "count-tgt2" with confidence 0.5
    When I count relations
    Then the relation count is 2

  @embedding
  Scenario: Store and verify pgvector embedding
    When I insert an ontologized chunk with id "emb-chunk"
    And I update the embedding for chunk "emb-chunk" with a 384-dimensional vector
    Then the embedding is stored for chunk "emb-chunk"
