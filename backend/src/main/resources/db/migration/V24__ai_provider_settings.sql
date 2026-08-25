-- AI provider settings (model choice + try-order), user-editable at runtime.
-- Supersedes ADR-0064's "no persistence" premise for this one piece of
-- state; see docs/adr/0065-ai-provider-settings.md. API keys stay
-- environment-only secrets (NFR-7) - this table only ever holds non-secret
-- preferences. No world_id: instance-global, like backup/import.

CREATE TABLE ai_provider_settings (
    provider   VARCHAR(50) PRIMARY KEY,
    model      VARCHAR(200),
    priority   INT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
