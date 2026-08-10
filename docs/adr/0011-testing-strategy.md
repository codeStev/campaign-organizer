# ADR-0011: Testing strategy (unit + Testcontainers)

- Status: Accepted
- Date: 2026-08-10

## Context
Critical paths (auth, worlds CRUD, auth enforcement) must be verified, and the
persistence layer uses Postgres-specific features and Flyway migrations that an
in-memory database would not exercise faithfully.

## Decision
Two layers of tests:
- **Unit tests** for pure logic with no Spring context (e.g. `JwtService`).
- **Integration tests** with `@SpringBootTest` + MockMvc against a real
  **PostgreSQL Testcontainer** (`@ServiceConnection`), so Flyway and JPA run
  exactly as in production. Requests use plain JSON strings and JsonPath to stay
  independent of the Jackson version Spring wires.

CI runs the full suite on every push (ADR-0004, GitHub Actions).

## Consequences
- High-fidelity DB tests; migrations are validated automatically.
- Requires Docker in the test/CI environment (available on GitHub runners).
- Slightly slower than H2, but avoids dialect mismatches.

## Alternatives considered
- **H2 in-memory:** fast, but diverges from Postgres (JSONB, FTS, types).
- **No integration tests:** unacceptable for the critical auth/data paths.
