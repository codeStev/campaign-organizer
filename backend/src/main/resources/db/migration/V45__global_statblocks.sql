CREATE TABLE global_statblocks (
    id UUID PRIMARY KEY,
    system_id UUID NOT NULL REFERENCES game_systems(id) ON DELETE RESTRICT,
    global_template_id UUID REFERENCES global_field_templates(id) ON DELETE RESTRICT,
    name VARCHAR(200) NOT NULL,
    stats JSONB NOT NULL DEFAULT '{}',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
