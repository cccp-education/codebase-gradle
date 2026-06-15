@epic12 @blog
Feature: EPIC 12 — Blog Narration Publique Automatique

  Background:
    Given un repertoire temporaire avec des sessions de test dans foundry

  @unit
  Scenario: BLG-1 — Extraire une session et générer un article JBake
    Given 1 session dans le dossier "public/test-borough/.agents/sessions"
    When le BlogArticleExtractor extrait les sessions
    Then 1 article est extrait
    And l'article a le titre "EPIC 7 Pipeline LLM — TDD complet"
    And l'article vient du borough "test-borough"
    And l'article a le numero de session "042"

  @unit
  Scenario: BLG-2 — Générer un fichier AsciiDoc valide avec en-têtes JBake
    Given 1 session extraite "042" du borough "test-borough"
    When le BlogArticleRenderer génère l'article dans le dossier blog
    Then le fichier généré existe
    And le fichier commence par le prefixe ":jbake-title:"
    And le fichier contient ":jbake-date:"
    And le fichier contient ":jbake-type: post"
    And le fichier contient ":jbake-status: published"
    And le fichier contient ":jbake-author: Cheroliv"
    And le fichier contient l'en-tête "== Contexte"
    And le fichier contient l'en-tête "== Réalisations"
    And le fichier contient l'en-tête "== Résultats des Tests"
    And le fichier contient l'en-tête "== Prochaine Session"

  @unit
  Scenario: BLG-3 — 3 sessions synthétiques → 3 articles générés
    Given 3 sessions dans "3" boroughs differents
    When le pipeline complet extraction et rendu est exécuté
    Then "3" articles sont générés
    And chaque article a un nom unique avec prefixe numerique de 4 chiffres
