-- Reusable NPC/monster statblocks. FR-18.
-- Stats are freeform key/value pairs stored as JSONB.

CREATE TABLE statblocks (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    article_id UUID REFERENCES articles(id) ON DELETE SET NULL,
    name       VARCHAR(200) NOT NULL,
    stats      JSONB NOT NULL DEFAULT '{}',
    notes      TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_statblocks_world ON statblocks (world_id, created_at DESC);
