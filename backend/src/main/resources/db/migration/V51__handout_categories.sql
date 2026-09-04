-- Handout categories (ADR-0105): same shape as map_categories — self-nesting,
-- ON DELETE CASCADE; the owner's category_id is ON DELETE SET NULL so a
-- handout survives its category being deleted.

CREATE TABLE handout_categories (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES handout_categories(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_handout_categories_world ON handout_categories (world_id);
CREATE INDEX idx_handout_categories_parent ON handout_categories (parent_id);

ALTER TABLE handouts
    ADD COLUMN category_id UUID REFERENCES handout_categories(id) ON DELETE SET NULL;

CREATE INDEX idx_handouts_category ON handouts (category_id);
