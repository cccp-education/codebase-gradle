CREATE TABLE IF NOT EXISTS invalid_chunks (
    id VARCHAR(64) PRIMARY KEY,
    source_file VARCHAR(512) NOT NULL,
    source_lines VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    errors JSONB NOT NULL,
    quarantined_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invalid_chunks_quarantined_at ON invalid_chunks (quarantined_at DESC);
CREATE INDEX IF NOT EXISTS idx_invalid_chunks_source_file ON invalid_chunks (source_file);
