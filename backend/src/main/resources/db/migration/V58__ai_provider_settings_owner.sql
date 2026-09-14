-- ai_provider_settings becomes per-account (ADR-0109) instead of
-- instance-global. Switches from `provider` as the bare primary key to a
-- surrogate UUID id + owner_id, matching this codebase's UUID-PK
-- convention everywhere else; the old uniqueness is now scoped per account.

ALTER TABLE ai_provider_settings ADD COLUMN id UUID;
UPDATE ai_provider_settings SET id = gen_random_uuid() WHERE id IS NULL;
ALTER TABLE ai_provider_settings ALTER COLUMN id SET NOT NULL;

ALTER TABLE ai_provider_settings DROP CONSTRAINT ai_provider_settings_pkey;
ALTER TABLE ai_provider_settings ADD PRIMARY KEY (id);

ALTER TABLE ai_provider_settings ADD COLUMN owner_id UUID REFERENCES accounts(id);

CREATE UNIQUE INDEX idx_ai_provider_settings_owner_provider
    ON ai_provider_settings (owner_id, provider);
