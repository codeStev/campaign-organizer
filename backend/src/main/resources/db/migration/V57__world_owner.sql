-- Every World becomes account-owned (ADR-0109). Nullable for now: existing
-- rows predate any account and are backfilled to the first-registered
-- (ADMIN) account at that registration, in application code — not here,
-- since no account exists yet when this migration runs.

ALTER TABLE worlds ADD COLUMN owner_id UUID REFERENCES accounts(id);

CREATE INDEX idx_worlds_owner_id ON worlds (owner_id);
