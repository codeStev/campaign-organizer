-- Folksonomy tags (FR-47, ADR-0083): freeform, world-scoped tags on
-- articles and statblocks. One row per (entity, tag); names are trimmed
-- and lower-cased before persistence, so uniqueness and lookups are
-- already case-normalized here.

CREATE TABLE entity_tags (
    id          UUID PRIMARY KEY,
    world_id    UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    entity_type VARCHAR(40) NOT NULL,
    entity_id   UUID NOT NULL,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_entity_tags_entity_name UNIQUE (world_id, entity_type, entity_id, name)
);

CREATE INDEX idx_entity_tags_world_name ON entity_tags (world_id, name);
CREATE INDEX idx_entity_tags_entity ON entity_tags (world_id, entity_type, entity_id);
