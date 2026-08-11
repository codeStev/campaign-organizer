-- Interactive maps and pins. See docs/adr/0018-interactive-maps-model.md.

CREATE TABLE maps (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    media_id   UUID REFERENCES media(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_maps_world_created ON maps (world_id, created_at DESC);

CREATE TABLE map_pins (
    id         UUID PRIMARY KEY,
    map_id     UUID NOT NULL REFERENCES maps(id) ON DELETE CASCADE,
    article_id UUID REFERENCES articles(id) ON DELETE SET NULL,
    label      VARCHAR(200),
    layer      VARCHAR(100),
    x          DOUBLE PRECISION NOT NULL,
    y          DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_map_pins_map ON map_pins (map_id);
