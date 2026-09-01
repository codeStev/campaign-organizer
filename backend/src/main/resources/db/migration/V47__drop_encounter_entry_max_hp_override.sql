-- HP is not a universal resource across game systems (Forbidden Lands,
-- Vaesen, ... don't track it); encounter entries stay generic and any
-- per-combatant resource tracking remains live/auto-detected at print
-- time (ADR-0069), not a persisted schema field.
ALTER TABLE encounter_entries DROP COLUMN max_hp_override;
