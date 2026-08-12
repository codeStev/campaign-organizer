-- Schema-driven character sheet templates. See docs/adr/0024-character-sheet-engine.md.
-- The `sections` field-definition is stored as JSONB (first JSONB use, per ADR-0003).

CREATE TABLE sheet_templates (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    system     VARCHAR(100),
    sections   JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sheet_templates_world ON sheet_templates (world_id, created_at DESC);
