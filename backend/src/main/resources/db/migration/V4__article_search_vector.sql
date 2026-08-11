-- Full-text search over articles. See docs/adr/0017-postgres-full-text-search.md.
-- Title is weighted A, HTML-stripped body weighted B. English config is used
-- explicitly so the expression is IMMUTABLE (required for a generated column).

ALTER TABLE articles ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
        setweight(to_tsvector('english', regexp_replace(coalesce(body, ''), '<[^>]+>', ' ', 'g')), 'B')
    ) STORED;

CREATE INDEX idx_articles_search ON articles USING GIN (search_vector);
