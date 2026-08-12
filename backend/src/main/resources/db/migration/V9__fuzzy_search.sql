-- Fuzzy article search via trigram matching. See docs/adr/0022-fuzzy-search.md.
-- pg_trgm is a trusted extension (PG 13+), so the database owner can enable it.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Speeds up ILIKE '%...%' and similarity() on titles.
CREATE INDEX idx_articles_title_trgm ON articles USING GIN (title gin_trgm_ops);
