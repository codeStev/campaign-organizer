-- Story arcs and their ordered beats. See docs/adr/0023-campaign-manager-model.md.

CREATE TABLE arcs (
    id          UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
    position    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_arcs_campaign_order ON arcs (campaign_id, position);

CREATE TABLE arc_beats (
    id          UUID PRIMARY KEY,
    arc_id      UUID NOT NULL REFERENCES arcs(id) ON DELETE CASCADE,
    article_id  UUID REFERENCES articles(id) ON DELETE SET NULL,
    session_id  UUID REFERENCES sessions(id) ON DELETE SET NULL,
    title       VARCHAR(200) NOT NULL,
    body        TEXT,
    done        BOOLEAN NOT NULL DEFAULT FALSE,
    position    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_beats_arc_order ON arc_beats (arc_id, position);
