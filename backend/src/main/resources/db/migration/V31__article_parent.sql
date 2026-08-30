-- Structural parent/child article nesting (e.g. a sub-location under its
-- parent location), independent of category_id which is a taxonomy.
-- SET NULL on parent delete: children survive as top-level articles.

ALTER TABLE articles
    ADD COLUMN parent_article_id UUID REFERENCES articles(id) ON DELETE SET NULL;

CREATE INDEX idx_articles_parent ON articles (parent_article_id);
