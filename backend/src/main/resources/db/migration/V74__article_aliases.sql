-- Article aliases (ADR-0116): alternate names for an article, feeding the
-- same [[link]] resolution index as its title/slug and findable by the
-- auto-link scan. Tier 2 (direct world_id column) per ADR-0114, carrying
-- its own RLS policy from birth like every table since foundry_connections
-- (V73). No cross-article uniqueness on the alias text itself - titles can
-- already collide across articles today (resolved by ArticleRefIndex's
-- existing last-write-wins precedence), so aliases follow the same lenient
-- rule; only an exact duplicate alias on the *same* article is rejected.

CREATE TABLE article_aliases (
    article_id  UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    world_id    UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    alias       VARCHAR(200) NOT NULL,
    PRIMARY KEY (article_id, alias)
);

CREATE INDEX idx_article_aliases_world_alias ON article_aliases (world_id, lower(alias));

ALTER TABLE article_aliases ENABLE ROW LEVEL SECURITY;
ALTER TABLE article_aliases FORCE ROW LEVEL SECURITY;
CREATE POLICY article_aliases_owner_isolation ON article_aliases
  USING (world_id IN (SELECT id FROM worlds));
