-- Account-scoped API tokens for machine clients (ADR-0119). Only the SHA-256 hash of the
-- secret is stored. No RLS, like account_sessions: the auth filter looks a token up by hash
-- before any account is known, so app.current_account_id (ADR-0114) cannot be set yet;
-- management queries are always scoped by account_id instead.

CREATE TABLE api_tokens (
    id            UUID PRIMARY KEY,
    account_id    UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    account_token_version INT NOT NULL,       -- accounts.token_version at creation; a bump invalidates the token
    name          VARCHAR(100) NOT NULL,
    token_hash    VARCHAR(64) NOT NULL UNIQUE,
    prefix        VARCHAR(16) NOT NULL,
    scopes        TEXT NOT NULL,                 -- comma-separated ApiTokenScope wire values
    created_at    TIMESTAMPTZ NOT NULL,
    expires_at    TIMESTAMPTZ,
    last_used_at  TIMESTAMPTZ,
    revoked_at    TIMESTAMPTZ
);

CREATE INDEX idx_api_tokens_account_id ON api_tokens (account_id);
