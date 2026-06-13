-- ============================================================================
-- V3__agentic_chunks.sql — Schéma pour les chunks ontologisés (EPIC Y-3 AgenticSchema)
-- Convention Flyway sans dépendance (MigrationRunner artisanal)
-- ============================================================================
-- Cible : PostgreSQL avec extension pgvector (Testcontainers pgvector/pgvector:pg17)
-- Pilote : R2DBC (r2dbc-postgresql, r2dbc-pool, r2dbc-spi)
-- ============================================================================
-- Stocke les AgenticChunk + OntologizedChunk dans une table unique dénormalisée
-- avec embedding vector(384) pour recherche sémantique.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS agentic_chunks (
    id                  TEXT PRIMARY KEY,
    source_file         TEXT NOT NULL,
    source_lines        TEXT NOT NULL,
    chunk_type          TEXT NOT NULL
        CHECK (chunk_type IN ('RULE', 'CONCEPT', 'PROCEDURE', 'METADATA', 'CONSTRAINT')),
    content             TEXT NOT NULL,
    verb                TEXT
        CHECK (verb IS NULL OR verb IN ('GENERER', 'COLLECTER', 'TRANSFORMER', 'DEPLOYER', 'INTERDIRE', 'VALIDER')),
    domain              TEXT,
    dag_level           TEXT
        CHECK (dag_level IS NULL OR dag_level IN ('N0', 'N1', 'N2', 'N3', 'N4')),
    circle              INTEGER,
    weight              DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    checksum            TEXT NOT NULL,
    taxonomy_section    TEXT NOT NULL DEFAULT 'UNKNOWN'
        CHECK (taxonomy_section IN ('PRINCIPES', 'TAXONOMIE', 'FORMAT_PIVOT', 'CONVENTION_OVER_CONFIGURATION', 'CONFIG_DOMAINE', 'MAPPING', 'ROADMAP_IMPLEMENTATION', 'DEPENDANCES', 'ORDRE_ATTAQUE', 'EXEMPLES_STDOUT', 'CONCLUSION', 'UNKNOWN')),
    ontology_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    embedding           vector(384),
    valid_from          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS chunk_relations (
    id              BIGSERIAL PRIMARY KEY,
    source_chunk_id TEXT NOT NULL REFERENCES agentic_chunks(id) ON DELETE CASCADE,
    target_chunk_id TEXT NOT NULL REFERENCES agentic_chunks(id) ON DELETE CASCADE,
    relation_type   TEXT NOT NULL
        CHECK (relation_type IN ('ENFORCES', 'CONFLICTS_WITH', 'DEPENDS_ON', 'REFINES')),
    confidence      DOUBLE PRECISION NOT NULL DEFAULT 0.5,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (source_chunk_id, target_chunk_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_agentic_chunks_domain ON agentic_chunks(domain);
CREATE INDEX IF NOT EXISTS idx_agentic_chunks_verb ON agentic_chunks(verb);
CREATE INDEX IF NOT EXISTS idx_agentic_chunks_dag_level ON agentic_chunks(dag_level);
CREATE INDEX IF NOT EXISTS idx_agentic_chunks_taxonomy ON agentic_chunks(taxonomy_section);
CREATE INDEX IF NOT EXISTS idx_agentic_chunks_valid ON agentic_chunks(valid_from, valid_until);
CREATE INDEX IF NOT EXISTS idx_chunk_relations_source ON chunk_relations(source_chunk_id);
CREATE INDEX IF NOT EXISTS idx_chunk_relations_target ON chunk_relations(target_chunk_id);
