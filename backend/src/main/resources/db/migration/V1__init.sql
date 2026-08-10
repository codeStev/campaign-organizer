-- Phase 0 schema. See docs/adr/0003-postgresql-datastore.md and
-- docs/adr/0012-flyway-migrations.md.

CREATE TABLE worlds (
    id          UUID PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_worlds_created_at ON worlds (created_at DESC);
