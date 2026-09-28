@page-retrieval
Feature: Page provenance retrieval (N1)
  As a codebase-gradle maintainer
  I want the page provenance ingested with a chunk to reach the retrieval and the Docs channel
  So that the augmented context cites the source page of a passage (EPIC CB-PAGE-PROVENANCE US-1/US-3)

  Background:
    Given a page retrieval world is initialized

  Scenario: Retrieve a chunk with a single page
    Given a page chunk with content "Hello world" and section path "Introduction" and heading level 1 and source document "test.txt" and page 10 is stored in the RAG
    When the page retrieval searches for query "Hello world"
    Then the page retrieval chunk has page 10

  Scenario: Retrieve a chunk with multiple pages
    Given a page chunk with content "Hello world" and section path "Introduction" and heading level 1 and source document "test.txt" and pages "10, 11" is stored in the RAG
    When the page retrieval searches for query "Hello world"
    Then the page retrieval chunk has pages "10, 11"

  Scenario: Retrieve a chunk with no page keeps backward compatibility
    Given a page chunk with content "Hello world" and section path "Introduction" and heading level 1 and source document "test.txt" and no page is stored in the RAG
    When the page retrieval searches for query "Hello world"
    Then the page retrieval chunk has no page

  Scenario: Docs channel exposes the page provenance of a retrieved chunk
    Given a page chunk with content "referentiel competences" and section path "Chapter 3" and heading level 1 and source document "livre.adoc" and pages "40, 41" is stored in the RAG
    When the page retrieval docs context is loaded for query "referentiel competences"
    Then the page retrieval docs channel exposes pages "40, 41"

  Scenario: Docs channel omits the pages fragment when the chunk has no page
    Given a page chunk with content "referentiel competences" and section path "Chapter 3" and heading level 1 and source document "livre.adoc" and no page is stored in the RAG
    When the page retrieval docs context is loaded for query "referentiel competences"
    Then the page retrieval docs channel has no pages fragment
