-- game_systems becomes per-account (ADR-0109) instead of instance-global;
-- name uniqueness moves from instance-wide to per-account, so two accounts
-- can each have their own "D&D 5e" entry.

ALTER TABLE game_systems ADD COLUMN owner_id UUID REFERENCES accounts(id);

DROP INDEX idx_game_systems_name_lower;
CREATE UNIQUE INDEX idx_game_systems_owner_name_lower ON game_systems (owner_id, lower(name));
