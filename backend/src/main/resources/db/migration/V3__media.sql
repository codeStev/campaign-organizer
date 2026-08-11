-- Media assets (images). Bytes live on the local volume; this table holds
-- metadata. See docs/adr/0016-media-storage-and-serving.md.

CREATE TABLE media (
    id           UUID PRIMARY KEY,
    world_id     UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    filename     VARCHAR(300) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes   BIGINT NOT NULL,
    storage_key  VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_media_world_created ON media (world_id, created_at DESC);
