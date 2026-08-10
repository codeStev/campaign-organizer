# ADR-0003: PostgreSQL as the datastore

- Status: Accepted
- Date: 2026-08-10

## Context
The domain has structured, relational data (worlds, articles, campaigns,
sessions) plus future flexible data (schema-driven character sheets). We need
one datastore that handles both and is easy to self-host.

## Decision
Use **PostgreSQL**. Relational tables model the core domain; `JSONB` columns
will hold flexible, schema-driven payloads (e.g. character sheets) when we get
there. Full-text search uses Postgres FTS initially.

## Consequences
- Single dependency covers relational + document-ish + search needs.
- Runs trivially as a container in the compose stack.
- Advanced search may later warrant a dedicated engine; revisit if needed.

## Alternatives considered
- **MongoDB:** flexible documents, but weaker for the relational core and joins.
- **SQLite:** simplest to host, but limited concurrency/JSONB/FTS ergonomics for
  a long-lived app.
