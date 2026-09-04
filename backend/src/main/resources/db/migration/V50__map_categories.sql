-- Map categories (ADR-0105): a separate taxonomy from Wiki's `categories`
-- table, per the user's explicit choice — same shape (self-nesting,
-- ON DELETE CASCADE), the owner's category_id is ON DELETE SET NULL so a
-- map survives its category being deleted.

CREATE TABLE map_categories (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES map_categories(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_map_categories_world ON map_categories (world_id);
CREATE INDEX idx_map_categories_parent ON map_categories (parent_id);

ALTER TABLE maps
    ADD COLUMN category_id UUID REFERENCES map_categories(id) ON DELETE SET NULL;

CREATE INDEX idx_maps_category ON maps (category_id);
