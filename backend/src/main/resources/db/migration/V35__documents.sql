-- General-purpose document instances (FR-50, ADR-0088). A third field-template
-- kind (DOCUMENT) alongside CHARACTER/STATBLOCK; a filled document has no
-- freeform-fallback mode, so it cascades with its template like character
-- sheets do (not SET NULL, like statblocks).

CREATE TABLE documents (
    id           UUID PRIMARY KEY,
    world_id     UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    template_id  UUID NOT NULL REFERENCES field_templates(id) ON DELETE CASCADE,
    campaign_id  UUID REFERENCES campaigns(id) ON DELETE SET NULL,
    name         VARCHAR(200) NOT NULL,
    field_values JSONB NOT NULL DEFAULT '{}',
    created_at   TIMESTAMPTZ NOT NULL,
    updated_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_documents_world ON documents (world_id, created_at DESC);
CREATE INDEX idx_documents_campaign ON documents (world_id, campaign_id);
CREATE INDEX idx_documents_template ON documents (template_id);
