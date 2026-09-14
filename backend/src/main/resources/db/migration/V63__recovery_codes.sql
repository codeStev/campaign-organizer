-- Single-use MFA recovery codes (ADR-0111), issued in a batch of 10 at the
-- end of every successful MFA enrollment (fresh or re-enrollment after a
-- previous code was spent). Serve two purposes: regaining access after a
-- lost second factor (POST /auth/mfa/verify-recovery-code), and resetting
-- a forgotten password without email infra (POST /auth/recover-password).
-- Hashed with the same BCrypt PasswordEncoder as account passwords — never
-- stored in plaintext, since a code is exactly as sensitive as a password.

CREATE TABLE account_recovery_codes (
    id         UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id),
    code_hash  VARCHAR(100) NOT NULL,
    used_at    TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_account_recovery_codes_account_id ON account_recovery_codes (account_id);
