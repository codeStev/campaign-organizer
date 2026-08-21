# ADR-0055: One-click instance backup (ZIP of pg_dump + media)

- Status: Accepted
- Date: 2026-08-21

## Context
The owner wants to move off the current environment onto self-hosted
infrastructure they haven't picked yet, and needs to get their real data
(worlds, articles, media) out safely in the meantime. `WorldExportController`
(`GET /worlds/{worldId}/export`) already exists, but it's world-scoped,
JSON-only, and excludes media files entirely (FR-22) — it round-trips
structured content, not a byte-for-byte transferable instance.

This calls for a separate, instance-wide backup capability, not an extension
of world export: a single download containing everything needed to stand the
app up again elsewhere.

## Decision
**One new endpoint, `GET /api/backup`, streams a ZIP containing:**
- `database.dump` — a real `pg_dump` of the whole database, custom format
  (`-Fc`): compressed, and restorable with `pg_restore` (supports selective
  and parallel restore, more forgiving across minor Postgres version drift
  than a raw SQL dump).
- `media/<relative path>` — every file under `APP_MEDIA_DIR`, recursively.

**Scope: backup only, not restore.** Restoring onto a new deployment is a
one-time, deliberate operation, documented below as a manual CLI procedure —
not a UI button. A wrong click on "Restore" is destructive; a wrong click on
"Backup" is a no-op. The owner asked to "transfer my progress," i.e. get data
out now; restore happens once, later, when a deployment target is chosen.

**Endpoint is instance-wide**, not per-world (`/api/backup`, not under
`/worlds/{id}`) — it captures the whole database. It's protected by the
existing default `anyRequest().authenticated()` in `SecurityConfig`; no
security config change needed (ADR-0006).

**New generic infra package `com.campaignorganizer.backup`**, flat like
`auth`/`config` — no domain/application/adapter rings. It isn't in
`ArchitectureTest`'s `CONTEXTS` set (`worldbuilding, campaign, characters,
media, whiteboard, interchange`), so it's automatically exempt from the
cross-context published-ports fitness function, same as `auth`/`config`
already are.

**`pg_dump` is shelled out via `ProcessBuilder`**, behind a small
`PgDumpRunner` interface (`InputStream dump()`), so `BackupService`'s
zip/media-composition logic is unit-testable with a fake dump source — no
real Postgres or `pg_dump` binary needed for the automated suite. Host/port/
database are parsed from `spring.datasource.url`; the password comes from
`spring.datasource.password` via `PGPASSWORD`.

**`backend/Dockerfile`'s runtime stage gains `postgresql-client`**
(`eclipse-temurin:25-jre` ships no Postgres client tools today), installed
from the base image's own Ubuntu repos — no third-party apt repo needed.
`pg_dump` only needs to be the same version or newer than the server it's
dumping (docker-compose.yml's `postgres:16-alpine`); the base image's default
package (verified: Ubuntu 26.04 ships `postgresql-client-18`) already
satisfies that, confirmed by actually dumping the real running server with
it before committing to this approach.

**No automated integration test shells a real `pg_dump`.** The Maven build
environment (`maven:3.9-eclipse-temurin-25`, used locally via Docker per this
project's established `mvn verify` pattern) doesn't carry PostgreSQL client
tools, and that's a separate concern from the runtime image. `BackupService`
is fully unit-tested with a fake `PgDumpRunner`; the real subprocess path
(actual `pg_dump` invocation, connection parameters, exit-code handling) is
verified manually against the running docker-compose stack — the only place
`pg_dump` is guaranteed present — the same way the Markdown-editor data
migration (ADR-0054) was verified against real data rather than trusted from
types alone.

**Streaming, not buffering.** The ZIP is written directly to the HTTP
response via `StreamingResponseBody` as `pg_dump` and the media files are
read, rather than assembled in memory first — the media directory can grow
arbitrarily large. Tradeoff: if `pg_dump` fails mid-stream, the already-sent
bytes can't be un-sent, so the download is a truncated/corrupt ZIP rather
than a clean HTTP error. Accepted for a personal, single-user tool; failures
are visible in `docker compose logs backend` (exit code + stderr logged).

## Restore procedure (manual, documented — not built)
1. Unzip the backup: `unzip campaign-organizer-backup-<ts>.zip -d restore/`.
2. Bring up a fresh stack (`docker compose up -d db`), then:
   `docker exec -i <db-container> pg_restore -U app -d campaign_organizer
   --clean --if-exists < restore/database.dump`.
3. Copy `restore/media/*` into the new deployment's `media-data` volume
   (e.g. `docker cp restore/media/. <backend-container>:/data/media/`).
4. Start the backend/frontend and verify.

## Consequences
- Gives the owner a genuine, restorable, one-click way to get all instance
  data (not just one world, and including binaries) off the current
  environment before picking a deployment target.
- Runtime Docker image grows slightly (adds `postgresql-client`); build
  time increases marginally for the extra apt install.
- A large media library means a slow, non-resumable single download; no
  incremental/scheduled backup is provided — acceptable for the stated need
  (one-time transfer), revisit if recurring automated backups are wanted
  later.

## Alternatives considered
- **Extend `WorldExportController`** to include media and wrap multiple
  worlds: rejected — that endpoint's JSON bundle format has no way to carry
  binary fidelity or non-world data (auth config aside, which is
  intentionally excluded anyway), and conflating "structured re-importable
  export" with "raw transferable backup" would complicate both.
- **One-click restore too**: rejected, see Decision — the destructive-action
  asymmetry isn't worth the convenience for an operation done once.
- **Scheduled/automatic backups**: out of scope — the ask was explicitly a
  one-click manual action to support an imminent one-time migration, not
  ongoing backup policy.
- **Copy `pg_dump`/`pg_restore` binaries from the `postgres:16-alpine`
  image** into the (glibc-based) runtime image via a multi-stage `COPY`:
  rejected — Alpine's musl-linked binaries aren't compatible with the
  Debian/Ubuntu-based `eclipse-temurin` runtime image's glibc.
