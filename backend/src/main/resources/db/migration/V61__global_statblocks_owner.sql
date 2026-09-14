-- global_statblocks becomes per-account (ADR-0109) instead of
-- instance-global.

ALTER TABLE global_statblocks ADD COLUMN owner_id UUID REFERENCES accounts(id);

CREATE INDEX idx_global_statblocks_owner_id ON global_statblocks (owner_id);
