# ADR-0007: Local-volume media storage

- Status: Accepted
- Date: 2026-08-10

## Context
Worldbuilding needs image uploads (maps, article art). An earlier proposal
included MinIO/S3, but the app is personal and self-hosted, where an object
store is extra operational weight.

## Decision
Store uploaded media on a **local filesystem directory** (`APP_MEDIA_DIR`),
mounted as a Docker named volume (`media-data`). The storage access is kept
behind a small abstraction so an S3 backend can be added later without touching
callers.

## Consequences
- One fewer service to run and back up (just a directory / volume).
- Backups are a filesystem copy alongside the Postgres dump.
- No built-in horizontal scaling of media; acceptable for single-node use.
- The storage abstraction preserves an escape hatch to S3 if requirements change.

## Alternatives considered
- **MinIO / S3:** cleaner blob separation and cloud portability, but unnecessary
  moving parts for personal hosting.
