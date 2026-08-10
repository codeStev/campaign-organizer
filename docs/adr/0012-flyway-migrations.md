# ADR-0012: Flyway schema migrations

- Status: Accepted
- Date: 2026-08-10

## Context
The schema will evolve across phases. We need reproducible, reviewable schema
changes rather than relying on Hibernate to mutate tables.

## Decision
Manage schema with **Flyway** versioned migrations under
`backend/src/main/resources/db/migration`. Hibernate is set to
`ddl-auto: validate` so the JPA mappings are checked against the
Flyway-managed schema but never alter it.

## Consequences
- Schema changes are explicit, ordered SQL files, reviewed in diffs.
- Prod and test share the exact same migrations (validated by Testcontainers).
- A mismatch between entities and migrations fails fast at startup.
- Requires discipline: every mapping change needs a matching migration.

## Alternatives considered
- **`ddl-auto: update`:** convenient but non-deterministic and unsafe for real
  data.
- **Liquibase:** comparable; Flyway's plain-SQL model is simpler here.
