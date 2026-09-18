@page-retrieval
Feature: Page retrieval
  As a user of the RAG
  I want to retrieve a chunk with its page number
  So that I can know the source page of the information

  Scenario: Retrieve a chunk with a single page
    Given a chunk with content "Hello world" and section path "Introduction" and heading level 1 and source document "test.txt" and page 10 is stored in the RAG
    When I search for the query "Hello world"
    Then the retrieved chunk should have page 10

  Scenario: Retrieve a chunk with multiple pages
    Given a chunk with content "Hello world" and section path "Introduction" and heading level 1 and source document "test.txt" and pages 10, 11 is stored in the RAG
    When I search for the query "Hello world"
    Then the retrieved chunk should have pages 10, 11

  Scenario: Retrieve a chunk with no page
    Given a chunk with content "Hello world" and section path "Introduction" and heading level 1 and source document "test.txt" and no page is stored in the RAG
    When I search for the query "Hello world"
    Then the retrieved chunk should have no page