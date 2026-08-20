-- Statblocks can optionally be driven by a STATBLOCK-kind field template
-- instead of freeform stats. Nullable: existing statblocks keep working
-- untouched. SET NULL (not CASCADE): deleting a template must not delete the
-- monster, it just falls back to freeform stats. See ADR-0052.

ALTER TABLE statblocks ADD COLUMN template_id UUID REFERENCES field_templates(id) ON DELETE SET NULL;

CREATE INDEX idx_statblocks_template ON statblocks (template_id);
