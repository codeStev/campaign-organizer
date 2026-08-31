-- Clocks (FR-48, ADR-0084): campaign-scoped segmented progress trackers.
-- Segments are stored as a JSONB payload; a clock is always loaded and
-- saved as a whole, never queried by individual segment (mirrors the
-- roll-tables/card-decks approach, ADR-0066).

CREATE TABLE clocks (
    id          UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    segments    JSONB NOT NULL DEFAULT '[]',
    position    INT NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_clocks_campaign_order ON clocks (campaign_id, position);
