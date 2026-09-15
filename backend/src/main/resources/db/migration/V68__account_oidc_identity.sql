-- SSO/OIDC login (ADR-0113): an account now has exactly one identity, either a local password
-- or an external (provider, subject) pair — never both, never neither. The CHECK constraint
-- backstops Account's own applyIdentity invariant at the DB layer.

ALTER TABLE accounts ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE accounts ADD COLUMN auth_provider VARCHAR(20);
ALTER TABLE accounts ADD COLUMN external_subject VARCHAR(255);

CREATE UNIQUE INDEX idx_accounts_provider_subject
    ON accounts (auth_provider, external_subject)
    WHERE auth_provider IS NOT NULL;

ALTER TABLE accounts ADD CONSTRAINT chk_accounts_identity
    CHECK (
        (password_hash IS NOT NULL AND auth_provider IS NULL AND external_subject IS NULL)
        OR (password_hash IS NULL AND auth_provider IS NOT NULL AND external_subject IS NOT NULL)
    );
