-- Global field template catalog (FR-55, ADR-0093): CHARACTER/STATBLOCK
-- templates can live in a world-independent catalog keyed by system,
-- instead of being duplicated per world. A dual nullable-FK reference
-- (world_template_id / global_template_id) replaces the single
-- template_id on character_sheets and statblocks, each guarded by a
-- CHECK constraint enforcing the right cardinality.

CREATE TABLE global_field_templates (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    kind VARCHAR(20) NOT NULL,
    system VARCHAR(100) NOT NULL,
    sections JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

ALTER TABLE character_sheets RENAME COLUMN template_id TO world_template_id;
ALTER TABLE character_sheets ALTER COLUMN world_template_id DROP NOT NULL;
ALTER TABLE character_sheets
    ADD COLUMN global_template_id UUID REFERENCES global_field_templates(id) ON DELETE RESTRICT;
ALTER TABLE character_sheets
    ADD CONSTRAINT chk_character_sheets_one_template
    CHECK (num_nonnulls(world_template_id, global_template_id) = 1);

ALTER TABLE statblocks RENAME COLUMN template_id TO world_template_id;
ALTER TABLE statblocks
    ADD COLUMN global_template_id UUID REFERENCES global_field_templates(id) ON DELETE RESTRICT;
ALTER TABLE statblocks
    ADD CONSTRAINT chk_statblocks_at_most_one_template
    CHECK (num_nonnulls(world_template_id, global_template_id) <= 1);
