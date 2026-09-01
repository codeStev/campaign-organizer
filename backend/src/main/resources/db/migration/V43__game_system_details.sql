-- Game system details (FR-57, ADR-0095): optional tagline, color badge,
-- and Markdown notes (rule references, house rules) on top of the bare
-- name ADR-0094 started with.

ALTER TABLE game_systems ADD COLUMN tagline VARCHAR(200);
ALTER TABLE game_systems ADD COLUMN color VARCHAR(20);
ALTER TABLE game_systems ADD COLUMN notes TEXT;
