-- Timelines and their dated events. See docs/adr/0019-timelines-model.md.

CREATE TABLE timelines (
    id          UUID PRIMARY KEY,
    world_id    UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_timelines_world_created ON timelines (world_id, created_at DESC);

CREATE TABLE timeline_events (
    id          UUID PRIMARY KEY,
    timeline_id UUID NOT NULL REFERENCES timelines(id) ON DELETE CASCADE,
    article_id  UUID REFERENCES articles(id) ON DELETE SET NULL,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    year        INTEGER NOT NULL,
    month       INTEGER,
    day         INTEGER,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_events_timeline_order ON timeline_events (timeline_id, year, month, day);
