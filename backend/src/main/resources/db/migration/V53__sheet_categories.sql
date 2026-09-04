-- Sheet categories (ADR-0105): one shared taxonomy across character sheets,
-- statblocks, documents, and field templates, backing Sheets' single merged
-- tree (no tabs). Same shape as the other category tables — self-nesting,
-- ON DELETE CASCADE; each owner's category_id is ON DELETE SET NULL so an
-- entity survives its category being deleted.

CREATE TABLE sheet_categories (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES sheet_categories(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sheet_categories_world ON sheet_categories (world_id);
CREATE INDEX idx_sheet_categories_parent ON sheet_categories (parent_id);

ALTER TABLE character_sheets
    ADD COLUMN category_id UUID REFERENCES sheet_categories(id) ON DELETE SET NULL;
CREATE INDEX idx_character_sheets_category ON character_sheets (category_id);

ALTER TABLE statblocks
    ADD COLUMN category_id UUID REFERENCES sheet_categories(id) ON DELETE SET NULL;
CREATE INDEX idx_statblocks_category ON statblocks (category_id);

ALTER TABLE documents
    ADD COLUMN category_id UUID REFERENCES sheet_categories(id) ON DELETE SET NULL;
CREATE INDEX idx_documents_category ON documents (category_id);

ALTER TABLE field_templates
    ADD COLUMN category_id UUID REFERENCES sheet_categories(id) ON DELETE SET NULL;
CREATE INDEX idx_field_templates_category ON field_templates (category_id);
