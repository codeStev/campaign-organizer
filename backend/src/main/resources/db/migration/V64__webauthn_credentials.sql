-- WebAuthn/passkey credentials (ADR-0111 follow-up): one row per registered
-- authenticator. Deliberately our own table, not Spring Security's
-- JdbcUserCredentialRepository schema — this project's persistence layer is
-- JPA/Hibernate everywhere else, and Spring's Jdbc-backed repository is a
-- hand-written-SQL alternative meant for apps that don't already have one.
-- A custom UserCredentialRepository adapter here maps to/from Spring's
-- CredentialRecord interface instead. sign_count backs clone-authenticator
-- detection (bumped on every successful assertion) — this is exactly the
-- field a real historical CVE (CVE-2023-45669, in a *different* WebAuthn
-- Spring integration) failed to persist correctly, so its round-trip
-- through this table is covered by an explicit test, not just trusted.
--
-- One account can only have one active WebAuthn credential at a time
-- (ADR-0111: exactly one MFA method per account, chosen at enrollment) —
-- the unique constraint on account_id enforces that at the database level
-- too, not just in application logic.

CREATE TABLE webauthn_credentials (
    id                           UUID PRIMARY KEY,
    account_id                   UUID NOT NULL UNIQUE REFERENCES accounts(id),
    credential_id                BYTEA NOT NULL UNIQUE,
    credential_type              VARCHAR(20) NOT NULL,
    public_key                   BYTEA NOT NULL,
    signature_count              BIGINT NOT NULL,
    uv_initialized                BOOLEAN NOT NULL,
    transports                   VARCHAR(100),
    backup_eligible               BOOLEAN NOT NULL,
    backup_state                  BOOLEAN NOT NULL,
    attestation_object           BYTEA NOT NULL,
    attestation_client_data_json BYTEA NOT NULL,
    label                         VARCHAR(200),
    created_at                    TIMESTAMPTZ NOT NULL,
    last_used_at                  TIMESTAMPTZ NOT NULL
);
