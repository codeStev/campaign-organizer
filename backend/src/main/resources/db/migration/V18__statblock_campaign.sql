-- Optionally scope a statblock to a campaign (e.g. a specific boss). See ADR-0032.
-- Null means shared. Deleting a campaign unlinks its statblocks.

ALTER TABLE statblocks
    ADD COLUMN campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL;

CREATE INDEX idx_statblocks_campaign ON statblocks (world_id, campaign_id);
