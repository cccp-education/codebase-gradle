@subgraph
Feature: EPIC SUBGRAPH — Real Graphify subgraph in augmented context
  As an agent consuming the augmented context
  I want the Graphify, Resource and graphRelations channels to carry real subgraph content
  So that LiveContextInjector and the composite context reflect the actual workspace graph

  Background:
    Given a synthetic graph.json with 3 nodes, 1 edge and 1 community

  @graphify-channel
  Scenario: Graphify channel carries the real subgraph content
    When I execute the multi-channel context graph
    Then the Graphify channel contains the subgraph header
    And the Graphify channel contains a node from the synthetic graph

  @resource-channel
  Scenario: Resource channel is fed from the same subgraph
    When I execute the multi-channel context graph
    Then the Resource channel contains the subgraph header
    And the Resource channel contains a node from the synthetic graph

  @graph-relations
  Scenario: graphRelations is populated when a graph file is provided
    When I load governance context with the graph file
    Then graphRelations contains the subgraph header
    And graphRelations contains a node from the synthetic graph

  @fallback
  Scenario: Missing graph file degrades gracefully
    Given a workspace without a graph file
    When I execute the multi-channel context graph
    Then the Graphify channel falls back to the missing file message
    And the Resource channel falls back to the missing file message