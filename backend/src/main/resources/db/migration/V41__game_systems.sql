-- Game system as a real entity (FR-56, ADR-0094): replaces the free-text
-- `system` column on field_templates/global_field_templates with a stable
-- FK. Schema only; V42 backfills existing system strings into rows here
-- and finishes tightening/cleanup.

CREATE TABLE game_systems (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_game_systems_name_lower ON game_systems (lower(name));

ALTER TABLE field_templates
    ADD COLUMN system_id UUID REFERENCES game_systems(id) ON DELETE SET NULL;

ALTER TABLE global_field_templates
    ADD COLUMN system_id UUID REFERENCES game_systems(id) ON DELETE RESTRICT;
