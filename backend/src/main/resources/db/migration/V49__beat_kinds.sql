-- Beat kinds (FR-61, ADR-0101): a world-scoped, GM-defined catalog of
-- named+colored beat kinds — not a fixed enum, same shape as game_systems'
-- name+color pair (V43), scoped per-world instead of globally.

CREATE TABLE beat_kinds (
    id UUID PRIMARY KEY,
    world_id UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_beat_kinds_world_name_lower ON beat_kinds (world_id, lower(name));

-- Informational reference, not structurally load-bearing (same treatment as
-- campaigns.system_id, V44): deleting a kind un-tags its beats instead of
-- being blocked or cascading.
ALTER TABLE arc_beats
    ADD COLUMN kind_id UUID REFERENCES beat_kinds(id) ON DELETE SET NULL;
