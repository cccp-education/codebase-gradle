@epic_v_9_5
Feature: Agentic Chunk Validator — Validation des chunks extraits
  As a codebase-gradle developer
  I want to validate that extracted chunks have consistent id, source, content, type, and checksum
  So that only well-formed chunks enter the repository

  @valid
  Scenario: A well-formed chunk extracted by AgenticChunker is valid
    Given an AGENT.adoc content with a rule "INTERDICTION FORMELLE de committer sans permission"
    When the AgenticChunker extracts chunks from the content
    And I validate each extracted chunk with ChunkValidator
    Then every chunk validation result is valid

  @blank_id
  Scenario: A chunk with blank id is rejected
    Given a chunk with a blank id
    When I validate the chunk with ChunkValidator
    Then the validation result is invalid
    And the validation errors contain "id"

  @checksum_mismatch
  Scenario: A chunk with a checksum not matching its content is rejected
    Given a chunk with content "INTERDICTION de leak" and a fake checksum "deadbeef"
    When I validate the chunk with ChunkValidator
    Then the validation result is invalid
    And the validation errors contain "checksum"

  @weight_out_of_range
  Scenario: A chunk with weight greater than 1 is rejected
    Given a chunk with weight 1.5
    When I validate the chunk with ChunkValidator
    Then the validation result is invalid
    And the validation errors contain "weight"

  @weight_negative
  Scenario: A chunk with negative weight is rejected
    Given a chunk with weight -0.2
    When I validate the chunk with ChunkValidator
    Then the validation result is invalid
    And the validation errors contain "weight"