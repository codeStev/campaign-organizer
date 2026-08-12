-- Relationship edges between articles. See docs/adr/0021-relationship-graph-model.md.

CREATE TABLE relationships (
    id               UUID PRIMARY KEY,
    world_id         UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    from_article_id  UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    to_article_id    UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    label            VARCHAR(100),
    directed         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_relationships_world ON relationships (world_id);
CREATE INDEX idx_relationships_from ON relationships (from_article_id);
CREATE INDEX idx_relationships_to ON relationships (to_article_id);
