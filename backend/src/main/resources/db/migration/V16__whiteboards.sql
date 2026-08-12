-- Free-form plotting whiteboards. See docs/adr/0027-whiteboards-model.md.
-- Nodes and edges are stored as JSONB; a board is loaded and saved as a whole.

CREATE TABLE whiteboards (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    nodes      JSONB NOT NULL DEFAULT '[]',
    edges      JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_whiteboards_world ON whiteboards (world_id, created_at DESC);
