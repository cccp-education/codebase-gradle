@doubt-quality
Feature: OCR-QUALITY US-4 — doubt metadata in the RAG ingestion and exposure
  As a codebase-gradle maintainer
  I want the RAG ingestion to carry OCR doubt metadata (confidence + doubtful flag)
  So that the augmented context annotates or excludes shaky OCR passages instead of citing them unknowingly

  Background:
    Given a doubt quality world is initialized

  Scenario: DoubtMetadata derives the doubtful flag from the confidence threshold
    Given a doubt metadata from confidence 0.2
    Then the doubt metadata confidence is 0.2
    And the doubt metadata is doubtful
    And the doubt metadata class is codebase.store.DoubtMetadata

  Scenario: DoubtMetadata keeps a confident chunk unflagged
    Given a doubt metadata from confidence 0.95
    Then the doubt metadata confidence is 0.95
    And the doubt metadata is not doubtful

  Scenario: DoubtMetadata explicit flag wins over the threshold heuristic
    Given a doubt metadata with confidence 0.9 and doubtful true
    Then the doubt metadata is doubtful
    And the doubt metadata confidence is 0.9

  Scenario: DoubtfulChunk pairs a chunk with its doubt metadata
    Given a doubtful chunk with source "book.adoc" and confidence 0.3
    Then the doubtful chunk doubt confidence is 0.3
    And the doubtful chunk is doubtful
    And the doubtful chunk source document is "book.adoc"

  Scenario: StoreStatements provides additive doubt DDL and bind contract
    When the doubt schema statements are generated
    Then the doubt schema has 2 statements
    And the doubt schema alters codex_documents with avg_confidence
    And the doubt schema alters codex_chunks with confidence and doubtful
    And the insert chunk with doubt template binds 7 parameters

  Scenario: RagVectorStore exposes doubt-aware search contract
    Given a rag store stub for doubt search
    When the doubt-aware search returns 2 chunks
    Then the search with doubt results carry confidence and doubtful fields

  Scenario: CompositeContextBuilder annotates doubtful chunks by default
    Given a rag store stub returning one doubtful and one confident chunk
    When the docs context is loaded with doubtful exclusion disabled
    Then the docs section contains the doubt marker
    And the docs section contains the confident chunk text
    And the docs section contains the doubtful chunk text

  Scenario: CompositeContextBuilder excludes doubtful chunks on demand
    Given a rag store stub returning one doubtful and one confident chunk
    When the docs context is loaded with doubtful exclusion enabled
    Then the docs section does not contain the doubt marker
    And the docs section does not contain the doubtful chunk text
    And the docs section contains the confident chunk text

  Scenario: DoubtExposure excludes all doubtful chunks when everything is shaky
    Given a rag store stub returning only doubtful chunks
    When the docs context is loaded with doubtful exclusion enabled
    Then the docs section reports all results were excluded