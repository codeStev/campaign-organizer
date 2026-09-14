-- global_field_templates becomes per-account (ADR-0109) instead of
-- instance-global, since the catalog is meant to be reused across one
-- account's own worlds, not shared between accounts.

ALTER TABLE global_field_templates ADD COLUMN owner_id UUID REFERENCES accounts(id);

CREATE INDEX idx_global_field_templates_owner_id ON global_field_templates (owner_id);
