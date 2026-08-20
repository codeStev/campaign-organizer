-- SheetTemplate is renamed to FieldTemplate: one template aggregate now serves
-- both character sheets and statblocks, distinguished by `kind`. See ADR-0052.
-- RENAME TO preserves data and the existing character_sheets.template_id FK.

ALTER TABLE sheet_templates RENAME TO field_templates;
ALTER INDEX idx_sheet_templates_world RENAME TO idx_field_templates_world;

ALTER TABLE field_templates ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'CHARACTER';

CREATE INDEX idx_field_templates_kind ON field_templates (world_id, kind, created_at DESC);
