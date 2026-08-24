@rag-socle
Feature: CDX-RAG-SOCLE — RagVectorStore socle + composite context Docs channel
  As a codebase-gradle maintainer
  I want the RAG store to live in the N1 socle (codebase.store) and the composite context Docs channel to consume it
  So that the N2->N1 inversion is dead and codex delegates retrieval to the socle

  Background:
    Given a rag socle world is initialized

  Scenario: RagVectorStore is the canonical socle store with default connection params
    When the rag socle store is instantiated with defaults
    Then the rag socle store is non-null
    And the rag socle store class is codebase.store.RagVectorStore

  Scenario: RagVectorStore accepts custom connection params
    When the rag socle store is instantiated with host "db.internal", port 6543, database "codex_prod", username "codex_user", password "secret"
    Then the rag socle store is non-null

  Scenario: RetrieveResult is the canonical socle retrieval type
    Given a retrieve result with chunkId 1, chunkIndex 0, chunkText "semantic chunk", sectionPath "Chapter 1", headingLevel 1, sourceDocument "doc.pdf", similarity 0.92
    Then the retrieve result chunkId is 1
    And the retrieve result chunkText is "semantic chunk"
    And the retrieve result similarity is 0.92
    And the retrieve result class is codebase.store.RetrieveResult

  Scenario: CompositeContextBuilder Docs channel reports unconfigured when store is null
    When the composite context builder is built with a null rag store
    And the docs context is loaded for query "architecture"
    Then the docs section reports the rag store is not configured

  Scenario: CompositeContextBuilder Docs channel degrades gracefully when the store throws
    Given a rag store stub that throws on search
    When the composite context builder is built with the throwing rag store stub
    And the docs context is loaded for query "database schema"
    Then the docs section reports the rag store is unavailable

  Scenario: CompositeContextBuilder Docs channel assembles results from the socle store
    Given a rag store stub returning 2 results for query "http client"
    When the composite context builder is built with the returning rag store stub
    And the docs context is loaded for query "http client"
    Then the docs section contains 2 result entries
    And the docs section contains the source document "doc.pdf"