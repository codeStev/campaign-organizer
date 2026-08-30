-- Screen-only prep flag (FR-46 follow-up): tracks whether a GM has revealed
-- this handout to players. Not an access-control gate (single-user app,
-- no player-facing surface) and doesn't affect printing.

ALTER TABLE handouts ADD COLUMN revealed BOOLEAN NOT NULL DEFAULT false;
