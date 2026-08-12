-- GM campaigns and their play sessions. See docs/adr/0023-campaign-manager-model.md.

CREATE TABLE campaigns (
    id          UUID PRIMARY KEY,
    world_id    UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    notes       TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_campaigns_world_created ON campaigns (world_id, created_at DESC);

CREATE TABLE sessions (
    id             UUID PRIMARY KEY,
    campaign_id    UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    session_number INTEGER,
    date           DATE,
    summary        TEXT,
    notes          TEXT,
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_sessions_campaign_order ON sessions (campaign_id, session_number, date);
