-- Cosmetic sandbox/brainstorming flag on worlds (FR-60, ADR-0100). No
-- functional effect (no exclusion from search/backup/export) — purely a
-- label shown in the world list and switcher.

ALTER TABLE worlds
    ADD COLUMN is_scratch BOOLEAN NOT NULL DEFAULT FALSE;
