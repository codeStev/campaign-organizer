-- Handouts (FR-46, ADR-0070): player-facing styled one-page printables —
-- letters, wanted posters, in-world newspaper pages. One row per handout;
-- the visual style is a fixed preset chosen at edit time.

CREATE TABLE handouts (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    preset     VARCHAR(40) NOT NULL,
    body       TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_handouts_world ON handouts (world_id, created_at DESC);
