-- Self-service WebAuthn credential management (ADR-0111 follow-up): an account can now
-- register more than one passkey (e.g. a phone and a backup security key). Enrollment of the
-- *first* credential is still gated to an account with no active MFA method, and adding any
-- credential after that still requires the caller's own request to already carry the MFA
-- factor (enforced in WebAuthnCredentialRepositoryAdapter, not here) — this migration only
-- lifts the database-level cap that made a second row impossible even for a legitimate owner.

ALTER TABLE webauthn_credentials DROP CONSTRAINT webauthn_credentials_account_id_key;

CREATE INDEX idx_webauthn_credentials_account_id ON webauthn_credentials (account_id);
