-- Character sheet instances. See docs/adr/0024-character-sheet-engine.md.
-- Values are stored as JSONB keyed by the template's field keys.
-- Column is `field_values` because `values` is a reserved SQL word.

CREATE TABLE character_sheets (
    id           UUID PRIMARY KEY,
    world_id     UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    template_id  UUID NOT NULL REFERENCES sheet_templates(id) ON DELETE CASCADE,
    article_id   UUID REFERENCES articles(id) ON DELETE SET NULL,
    name         VARCHAR(200) NOT NULL,
    field_values JSONB NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_character_sheets_world ON character_sheets (world_id, created_at DESC);
CREATE INDEX idx_character_sheets_template ON character_sheets (template_id);
