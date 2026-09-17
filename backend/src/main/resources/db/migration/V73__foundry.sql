-- Foundry VTT push integration (ADR-0115): per-world relay connection config
-- (bring-your-own relay - no default/shared instance), plus a push-tracking
-- table for idempotency/"last pushed" UI feedback. Both are Tier 2 (direct
-- world_id column) per ADR-0114 - RLS is added inline here rather than in a
-- separate tiered migration, since that tiering was a one-time retrofit of
-- tables that predated RLS; new tables carry their own policy from birth.

CREATE TABLE foundry_connections (
    world_id            UUID PRIMARY KEY REFERENCES worlds(id) ON DELETE CASCADE,
    relay_base_url      TEXT NOT NULL,
    client_id           TEXT NOT NULL,
    api_key_encrypted   VARCHAR(500) NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL,
    updated_at          TIMESTAMPTZ NOT NULL
);

ALTER TABLE foundry_connections ENABLE ROW LEVEL SECURITY;
ALTER TABLE foundry_connections FORCE ROW LEVEL SECURITY;
CREATE POLICY foundry_connections_owner_isolation ON foundry_connections
  USING (world_id IN (SELECT id FROM worlds));

CREATE TABLE foundry_pushed_documents (
    id                  UUID PRIMARY KEY,
    world_id            UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    entity_type         VARCHAR(20) NOT NULL,      -- ARTICLE | HANDOUT | ROLL_TABLE | CARD_DECK
    entity_id           UUID NOT NULL,
    foundry_document_id VARCHAR(16) NOT NULL,      -- the stable Foundry _id
    pushed_at           TIMESTAMPTZ NOT NULL,
    UNIQUE (world_id, entity_type, entity_id)
);

CREATE INDEX idx_foundry_pushed_documents_lookup
  ON foundry_pushed_documents (world_id, entity_type, entity_id);

ALTER TABLE foundry_pushed_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE foundry_pushed_documents FORCE ROW LEVEL SECURITY;
CREATE POLICY foundry_pushed_documents_owner_isolation ON foundry_pushed_documents
  USING (world_id IN (SELECT id FROM worlds));
