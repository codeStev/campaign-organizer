-- Optionally scope a character sheet to a campaign (the party). See ADR-0031.
-- Null means unassigned/shared. Deleting a campaign unlinks its sheets.

ALTER TABLE character_sheets
    ADD COLUMN campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL;

CREATE INDEX idx_character_sheets_campaign ON character_sheets (world_id, campaign_id);
