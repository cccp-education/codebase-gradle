@ocr @epic_ocr
Feature: OCR — Extraction de texte assistée IA

  Scénarios de test pour la tâche Gradle `ocrDocument`.
  Utilise FakeOcrEngine (pas d'appel réseau, pas de clé API).
  En production, GeminiVisionEngine remplacera FakeOcrEngine.

  @unit
  Scenario: OCR with French text produces structured AsciiDoc
    Given an OCR test file "scan-sample.txt" with text "Ceci est un document test."
    When I OCR "scan-sample.txt" in French
    Then the OCR result for "scan-sample" exists
    And the OCR result for "scan-sample" contains "= Document OCRisé"

  @unit
  Scenario: OCR with English text includes correct language metadata
    Given an OCR test file "english-doc.txt" with text "This is an English document."
    When I OCR "english-doc.txt" in English
    Then the OCR result for "english-doc" exists
    And the OCR result for "english-doc" contains ":langue: en"

  @unit
  Scenario: OCR of empty file still produces output
    Given an OCR test file "empty-doc.txt" with text ""
    When I OCR "empty-doc.txt" in French
    Then the OCR result for "empty-doc" exists

  @unit
  Scenario: Task is registered by CodebasePlugin
    Given the codebase plugin is applied
    When I check for task "ocrDocument"
    Then task "ocrDocument" should be registered
    And task "ocrDocument" should be in group "collect"

  @unit
  Scenario: DSL output format respected
    Given an OCR test file "fmt.txt" with text "Hello world."
    When I OCR "fmt.txt" in French with format "markdown"
    Then the OCR result for "fmt" ends with ".md"

  @unit @epic_ocr_1
  Scenario: Gemini key pool rotates when HTTP 429 is received
    Given a Gemini key pool with 3 keys "key-A,key-B,key-C"
    When key "key-A" receives HTTP 429
    Then the pool marks "key-A" as rate-limited
    And the next available key is "key-B"

  @unit @epic_ocr_1
  Scenario: Gemini key pool skips all rate-limited keys best-effort
    Given a Gemini key pool with 2 keys "key-A,key-B"
    When key "key-A" receives HTTP 429
    And key "key-B" receives HTTP 429
    Then the pool marks "key-A" as rate-limited
    And the pool marks "key-B" as rate-limited
    And the next available key is one of "key-A,key-B"

  @unit @epic_ocr_1
  Scenario: Gemini key pool reset clears rate-limited markers
    Given a Gemini key pool with 2 keys "key-A,key-B"
    When key "key-A" receives HTTP 429
    And the Gemini key pool usage is reset
    Then the pool does not mark "key-A" as rate-limited
    And the next available key is "key-A"

  @unit @epic_ocr_1
  Scenario: Gemini pool factory maps env vars GEMINI_API_KEY_1..N to pool instances
    Given env vars with GEMINI_API_KEY_1="env-secret-1" and GEMINI_API_KEY_2="env-secret-2"
    When I build a Gemini pool from env vars
    Then the pool has 2 instances
    And the first instance key is "env-secret-1"
    And the second instance key is "env-secret-2"

  @unit @epic_ocr_1
  Scenario: Gemini pool factory skips empty env vars
    Given env vars with GEMINI_API_KEY_1="env-secret-1" and GEMINI_API_KEY_2="" and GEMINI_API_KEY_3="env-secret-3"
    When I build a Gemini pool from env vars
    Then the pool has 2 instances
    And the first instance key is "env-secret-1"
    And the second instance key is "env-secret-3"

  @unit @epic_ocr_1
  Scenario: Multi-account Gemini pool rotates across accounts
    Given a multi-account Gemini pool with 2 accounts and keys "account-1:key-A1,key-A2" and "account-2:key-B1,key-B2"
    Then the multi-account pool has 2 accounts
    And the multi-account pool has 4 total instances
    When I get 4 instances from the multi-account pool
    Then all 4 instances should have distinct ids

  @unit @epic_ocr_1
  Scenario: Multi-account Gemini pool marks rate-limited key in correct account
    Given a multi-account Gemini pool with 2 accounts and keys "account-1:key-A1,key-A2" and "account-2:key-B1"
    When the first instance is marked rate-limited
    Then the next instance from the multi-account pool is not the rate-limited one

  @unit @epic_ocr_1
  Scenario: Multi-account pool from env vars GEMINI_ACCOUNT_N_API_KEY_M
    Given env vars with GEMINI_ACCOUNT_1_API_KEY_1="a1k1" and GEMINI_ACCOUNT_1_API_KEY_2="a1k2" and GEMINI_ACCOUNT_2_API_KEY_1="a2k1"
    When I build a multi-account Gemini pool from env vars
    Then the multi-account pool has 2 accounts
    And the multi-account pool has 3 total instances

  @unit @epic_ocr_2
  Scenario: OCR with Tesseract provider is rejected — software OCR belongs to codex
    Given an OCR test file "tess-scan.png" with text "fake png for tesseract"
    When I OCR "tess-scan.png" with provider "tesseract"
    Then an error is raised with message containing "codex"

  @unit @epic_ocr_2
  Scenario: OCR raises error when both Gemini and Ollama fail — no software fallback in codebase
    Given an OCR test file "tess-fallback.png" with text "fake png for tess fallback"
    When I OCR "tess-fallback.png" with provider "gemini+ollama" and Gemini and Ollama fail
    Then an error is raised with message containing "codex"

  @unit @epic_ocr_2b
  Scenario: OCR with Ollama provider produces structured AsciiDoc
    Given an OCR test file "ollama-scan.png" with text "fake png for ollama"
    When I OCR "ollama-scan.png" with provider "ollama"
    Then the OCR result for "ollama-scan" exists
    And the OCR result for "ollama-scan" contains "FakeOllamaOcrProvider"

  @unit @epic_ocr_2b
  Scenario: OCR with gemini+ollama fallback uses Gemini first
    Given an OCR test file "fallback-scan.png" with text "fake png for fallback"
    When I OCR "fallback-scan.png" with provider "gemini+ollama"
    Then the OCR result for "fallback-scan" exists
    And the OCR result for "fallback-scan" contains "FakeVisionProvider"

  @unit @epic_ocr_2b
  Scenario: OCR with gemini+ollama falls back to Ollama when Gemini fails
    Given an OCR test file "gemini-fail.png" with text "fake png for gemini failure"
    When I OCR "gemini-fail.png" with provider "gemini+ollama" and Gemini fails
    Then the OCR result for "gemini-fail" exists
    And the OCR result for "gemini-fail" contains "FakeOllamaOcrProvider"

  @unit @epic_ocr_3
  Scenario: OCR batch with inputDir processes multiple files
    Given an OCR test directory "batch-scans" with files:
      | file1.txt | Content A |
      | file2.txt | Content B |
      | file3.txt | Content C |
    When I OCR the directory "batch-scans" in French
    Then the OCR result for "file1" exists
    And the OCR result for "file2" exists
    And the OCR result for "file3" exists
    And the OCR result for "file1" contains "Content A"
    And the OCR result for "file2" contains "Content B"
    And the OCR result for "file3" contains "Content C"

  @unit @epic_ocr_3
  Scenario: OCR batch with empty directory throws error
    Given an OCR test directory "empty-dir" with no files
    When I OCR the directory "empty-dir" in French
    Then an error is raised with message containing "Aucun fichier d'entrée"

  @unit @epic_ocr_3
  Scenario: OCR batch with inputFile takes priority over inputDir
    Given an OCR test directory "mixed-dir" with files:
      | batch.txt | Batch content |
    And an OCR test file "single.txt" with text "Single content"
    When I OCR file "single.txt" with directory "mixed-dir" in French
    Then the OCR result for "single" exists
    And the OCR result for "batch" does not exist
    And the OCR result for "single" contains "Single content"

  @unit @epic_ocr_4
  Scenario: OCR with anonymization replaces emails
    Given an OCR test file "pii-doc.txt" with text "Contact: jean.dupont@example.com"
    When I OCR "pii-doc.txt" in French with anonymization enabled
    Then the OCR result for "pii-doc" exists
    And the OCR result for "pii-doc" does not contain "jean.dupont@example.com"
    And the OCR result for "pii-doc" contains "***@anonymous.com"

  @unit @epic_ocr_4
  Scenario: OCR with anonymization replaces phone numbers
    Given an OCR test file "phone-doc.txt" with text "Tel: 06 12 34 56 78"
    When I OCR "phone-doc.txt" in French with anonymization enabled
    Then the OCR result for "phone-doc" exists
    And the OCR result for "phone-doc" does not contain "06 12 34 56 78"

  @unit @epic_ocr_4
  Scenario: OCR without anonymization preserves PII
    Given an OCR test file "keep-pii.txt" with text "Email: alice@acme.com, Tel: 01 23 45 67 89"
    When I OCR "keep-pii.txt" in French
    Then the OCR result for "keep-pii" exists
    And the OCR result for "keep-pii" contains "alice@acme.com"
    And the OCR result for "keep-pii" contains "01 23 45 67 89"

  @integration @epic_ocr_4_ingest
  Scenario: OCR ingest chunks and embeds a single document
    Given an OCR output file "report_ocr.adoc" with content:
      """
      = Rapport OCRisé
      :langue: fr

      == Introduction

      Ceci est un rapport de test pour l'ingestion OCR.

      == Analyse

      Les résultats montrent une amélioration de 15%.
      """
    When I ingest OCR output into pgvector
    Then at least 1 document is indexed
    And at least 1 chunk is indexed
    And all chunks have embeddings

  @integration @epic_ocr_4_ingest
  Scenario: OCR ingest multiple documents
    Given OCR output files:
      | doc1_ocr.adoc | = Document 1\n\nContenu du premier document. |
      | doc2_ocr.adoc | = Document 2\n\nContenu du second document.  |
      | doc3_ocr.adoc | = Document 3\n\nContenu du troisième document. |
    When I ingest OCR output into pgvector
    Then exactly 3 documents are indexed
    And all chunks have embeddings

  @integration @epic_ocr_4_ingest
  Scenario: OCR ingest result is queryable via RAG
    Given an OCR output file "finance_ocr.adoc" with content:
      """
      = Rapport Financier OCRisé

      == Chiffre d'affaires

      Le chiffre d'affaires du Q1 2026 est de 1.2 million d'euros.
      La marge brute est de 45 pourcent.

      == Effectif

      L'effectif est de 42 collaborateurs.
      """
    When I ingest OCR output into pgvector
    And I query pgvector for "chiffre d'affaires Q1 2026"
    Then the RAG query returns at least 1 result
    And at least 1 result contains "chiffre d'affaires" or "Q1"

  @integration @epic_ocr_4_ingest
  Scenario: OCR ingest skips non-adoc files
    Given OCR output files:
      | doc_ocr.adoc | = Document\n\nContenu AsciiDoc. |
      | doc_ocr.md   | # Document\n\nContenu Markdown.  |
      | doc_ocr.txt  | Document texte.                   |
    When I ingest OCR output into pgvector
    Then exactly 1 document is indexed

  @integration @epic_ocr_4_ingest
  Scenario: OCR ingest with anonymized content is queryable
    Given an OCR output file "anonymized_ocr.adoc" with content:
      """
      = Document OCRisé (Anonymisé)

      Contact: ***@anonymous.com
      Tel: ***

      == Projet Alpha

      Le projet Alpha a livré la version 2.0 en production.
      Les tests de performance montrent une latence de 12ms.
      """
    When I ingest OCR output into pgvector
    And I query pgvector for "projet Alpha version production"
    Then the RAG query returns at least 1 result
    And at least 1 result contains "Alpha" or "production"

  @unit @epic_ocr_4_5
  Scenario: OCR collects metrics per file
    Given an OCR test file "metrics-doc.txt" with text "Metrics test content"
    When I OCR "metrics-doc.txt" in French
    Then the OCR result for "metrics-doc" exists
    And the OCR metrics report exists
    And the OCR metrics report contains "metrics-doc.txt"

  @unit @epic_ocr_4_5
  Scenario: OCR batch collects metrics for all files
    Given an OCR test directory "batch-metrics" with files:
      | a.txt | Content A |
      | b.txt | Content B |
    When I OCR the directory "batch-metrics" in French
    Then the OCR result for "a" exists
    And the OCR result for "b" exists
    And the OCR metrics report exists
    And the OCR metrics report contains "a.txt"
    And the OCR metrics report contains "b.txt"

  @unit @epic_ocr_4_5
  Scenario: OCR with ollama provider tracks zero cost in metrics
    Given an OCR test file "ollama-metrics.png" with text "fake png for ollama metrics"
    When I OCR "ollama-metrics.png" with provider "ollama"
    Then the OCR result for "ollama-metrics" exists
    And the OCR metrics report exists
    And the OCR metrics report contains "ollama"
    And the OCR metrics report contains "0.000000"

  @unit @epic_ocr_4_5
  Scenario: OCR with anonymization tracks replacements in metrics
    Given an OCR test file "pii-metrics.txt" with text "Contact: jean.dupont@example.com"
    When I OCR "pii-metrics.txt" in French with anonymization enabled
    Then the OCR result for "pii-metrics" exists
    And the OCR metrics report exists
    And the OCR metrics report contains "Remplacements anonymisation"

  @unit @epic_ocr_4_5
  Scenario: OCR metrics report contains duration and cost
    Given an OCR test file "cost-doc.txt" with text "Cost tracking test content"
    When I OCR "cost-doc.txt" in French
    Then the OCR result for "cost-doc" exists
    And the OCR metrics report exists
    And the OCR metrics report contains "Durée totale"
    And the OCR metrics report contains "Coût total estimé"
