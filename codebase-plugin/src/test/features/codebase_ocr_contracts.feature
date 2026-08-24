@ocr-contracts
Feature: CDX-OCR-CONTRACTS US-4 — OCR boundary via the N0 ocr-contracts port
  As a codebase-gradle maintainer
  I want the N1 socle to expose AI-assisted OCR through the N0 `contracts.ocr.OcrEngine` port
  So that codex (N2) can inject AI vision without an N2->N1 cycle and software OCR stays in codex

  Background:
    Given an OCR contracts world is initialized

  Scenario: Fake AI engine returns structured AsciiDoc through the N0 port
    When the vision provider stub returns "= Page un" for a PNG request in French
    And the N0 OCR engine adapter processes the request
    Then the OCR result structured text is "= Page un"
    And the OCR result class is contracts.ocr.OcrResult
    And the OCR result confidence is 1.0

  Scenario: Request metadata is mirrored into the OCR result
    Given a JPEG request in English processed with model "gemini-2.5-pro"
    When the N0 OCR engine adapter processes the request
    Then the OCR result language is "en"
    And the OCR result source format is "image/jpeg"
    And the OCR result model is "gemini-2.5-pro"

  Scenario: The engine port is stateless and reusable across successive calls
    Given two successive requests through the same N0 engine instance
    When the N0 OCR engine adapter processes both requests
    Then both OCR results carry their own request text
    And the vision provider stub was called 2 times

  Scenario: Software OCR provider tesseract is rejected — boundary preserved
    Given a scan file "contracts-tess.png" submitted with software provider "tesseract"
    When the codebase OCR task processes the file
    Then an OCR contracts error is raised containing "codex"
    And no OCR output file exists for "contracts-tess"

  Scenario: Both AI providers failing raises the AI-only boundary message — no silent Tesseract fallback
    Given a scan file "contracts-fallback.png" with providers gemini and ollama both failing
    When the codebase OCR task processes the file
    Then an OCR contracts error is raised containing "collectOcr"
