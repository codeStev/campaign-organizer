-- Beats can reference statblocks directly (no article needed), mirroring
-- beat_articles (ADR-0043). Enables implicit campaign→statblock linkage.
CREATE TABLE beat_statblocks (
    beat_id      UUID NOT NULL REFERENCES arc_beats(id) ON DELETE CASCADE,
    statblock_id UUID NOT NULL REFERENCES statblocks(id) ON DELETE CASCADE
);
CREATE INDEX idx_beat_statblocks_beat ON beat_statblocks (beat_id);
CREATE INDEX idx_beat_statblocks_statblock ON beat_statblocks (statblock_id);
