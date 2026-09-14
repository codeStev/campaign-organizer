-- Mandatory MFA (ADR-0111): every account must have an active second
-- factor before its token carries the MFA authority (see
-- AuthorizationManagerFactories.multiFactor() wiring in SecurityConfig).
-- mfa_method defaults to NONE for both brand-new registrations (which
-- complete enrollment right after their first login) and the accounts that
-- already existed before this migration (forced through enrollment the
-- same way on their next login). TOTP secrets are encrypted at rest via
-- the app's TextEncryptor bean (Spring Security Crypto's Encryptors.delux)
-- — never stored in plaintext. The "pending" secret
-- holds an in-progress, unconfirmed setup attempt separately from the
-- confirmed/active one, so an abandoned setup never overwrites a working
-- secret.

ALTER TABLE accounts
    ADD COLUMN mfa_method VARCHAR(20) NOT NULL DEFAULT 'NONE',
    ADD COLUMN totp_secret_encrypted VARCHAR(500),
    ADD COLUMN totp_secret_pending_encrypted VARCHAR(500);
