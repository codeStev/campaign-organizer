-- Phase 1 wiki schema: categories (hierarchical) and articles.
-- See docs/adr/0013-article-content-model.md.

CREATE TABLE categories (
    id         UUID PRIMARY KEY,
    world_id   UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    parent_id  UUID REFERENCES categories(id) ON DELETE CASCADE,
    name       VARCHAR(200) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_categories_world ON categories (world_id);
CREATE INDEX idx_categories_parent ON categories (parent_id);

CREATE TABLE articles (
    id          UUID PRIMARY KEY,
    world_id    UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    category_id UUID REFERENCES categories(id) ON DELETE SET NULL,
    title       VARCHAR(200) NOT NULL,
    slug        VARCHAR(220) NOT NULL,
    template    VARCHAR(50) NOT NULL DEFAULT 'GENERIC',
    body        TEXT,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_articles_world_slug UNIQUE (world_id, slug)
);

CREATE INDEX idx_articles_world_created ON articles (world_id, created_at DESC);
CREATE INDEX idx_articles_category ON articles (category_id);
