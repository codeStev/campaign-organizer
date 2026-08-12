-- Append-only article revision snapshots. See docs/adr/0026-article-revision-history.md.

CREATE TABLE article_revisions (
    id         UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL,
    slug       VARCHAR(220) NOT NULL,
    template   VARCHAR(50) NOT NULL,
    body       TEXT,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_article_revisions_article ON article_revisions (article_id, created_at DESC);
