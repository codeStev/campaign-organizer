-- Account roster (ADR-0109/ADR-0110): replaces the single shared
-- APP_PASSWORD with real per-account credentials. token_version supports
-- immediate revocation (password change/reset, role change, disable, or an
-- explicit "log out everywhere") without server-side session storage —
-- JwtAuthFilter compares the JWT's `ver` claim against this column on
-- every request. failed_attempts/locked_until back a short lockout after
-- repeated bad logins.

CREATE TABLE accounts (
    id              UUID PRIMARY KEY,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    role            VARCHAR(20) NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    token_version   INT NOT NULL DEFAULT 0,
    failed_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX idx_accounts_email_lower ON accounts (lower(email));
