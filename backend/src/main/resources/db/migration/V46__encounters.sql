CREATE TABLE encounters (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_encounters_campaign ON encounters (campaign_id);

CREATE TABLE encounter_entries (
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE,
    statblock_id UUID NOT NULL REFERENCES statblocks(id) ON DELETE CASCADE,
    quantity INT NOT NULL DEFAULT 1,
    max_hp_override INT
);
CREATE INDEX idx_encounter_entries_encounter ON encounter_entries (encounter_id);
CREATE INDEX idx_encounter_entries_statblock ON encounter_entries (statblock_id);

CREATE TABLE beat_encounters (
    beat_id      UUID NOT NULL REFERENCES arc_beats(id) ON DELETE CASCADE,
    encounter_id UUID NOT NULL REFERENCES encounters(id) ON DELETE CASCADE
);
CREATE INDEX idx_beat_encounters_beat ON beat_encounters (beat_id);
CREATE INDEX idx_beat_encounters_encounter ON beat_encounters (encounter_id);
