-- Self-service session/device tracking (ADR-0112): a second, finer-grained revocation layer
-- alongside accounts.token_version. One row per full-factor (PASSWORD+MFA) token issued — a
-- PASSWORD-only pending-MFA token never gets a row here, since it isn't a "device logged in"
-- yet. `id` is the token's own jti, so JwtAuthFilter can look a presented token up directly.

CREATE TABLE account_sessions (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    user_agent TEXT,
    ip_address TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ
);

CREATE INDEX idx_account_sessions_account_id ON account_sessions (account_id);
