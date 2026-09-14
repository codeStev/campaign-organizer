-- Backs the stateless PublicKeyCredentialCreationOptionsRepository/
-- PublicKeyCredentialRequestOptionsRepository implementations Spring
-- Security's WebAuthn support needs between a ceremony's "options" step
-- and its "verify" step (ADR-0111 follow-up) — replacing the session-backed
-- defaults, since this app has no HttpSession at all. Keyed by account_id
-- rather than an opaque id/cookie: every WebAuthn endpoint here requires a
-- PASSWORD-factor bearer token first (same as TOTP), so the account is
-- already known from the SecurityContext by the time these repositories
-- run — no need to invent a separate session-like identifier. purpose
-- distinguishes an in-progress registration from an in-progress
-- authentication challenge; only one of either can be outstanding per
-- account at a time (starting a new ceremony replaces any abandoned one).

CREATE TABLE webauthn_challenges (
    account_id   UUID PRIMARY KEY REFERENCES accounts(id),
    purpose      VARCHAR(20) NOT NULL,
    options_json TEXT NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
);
