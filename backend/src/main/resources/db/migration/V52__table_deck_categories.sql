-- Table/deck categories (ADR-0105): one shared taxonomy for roll tables and
-- card decks, since they already live in one Tables & Decks screen. Same
-- shape as the other category tables — self-nesting, ON DELETE CASCADE; the
-- owner's category_id is ON DELETE SET NULL so a table/deck survives its
-- category being deleted.

CREATE TABLE table_deck_categories (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES table_deck_categories(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_table_deck_categories_world ON table_deck_categories (world_id);
CREATE INDEX idx_table_deck_categories_parent ON table_deck_categories (parent_id);

ALTER TABLE roll_tables
    ADD COLUMN category_id UUID REFERENCES table_deck_categories(id) ON DELETE SET NULL;

CREATE INDEX idx_roll_tables_category ON roll_tables (category_id);

ALTER TABLE card_decks
    ADD COLUMN category_id UUID REFERENCES table_deck_categories(id) ON DELETE SET NULL;

CREATE INDEX idx_card_decks_category ON card_decks (category_id);
