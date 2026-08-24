# ADR-0061: Backup import (additive and full-overwrite), structured bundle replaces pg_dump

- Status: Accepted
- Date: 2026-08-24

## Context
ADR-0055 gave `/api/backup` a `pg_dump` (plain SQL) + media ZIP, backup-only
by design — restore was documented as a manual `docker exec`/`psql`
procedure, deliberately not built, because "a wrong click on Restore is
destructive." That manual procedure turned out to be exactly as painful as
it sounds in practice (this session walked through it by hand to import a
downloaded backup into a running deployment). The owner wants in-app import,
in two modes:

- **Additive** — add the backup's world(s) as new data alongside whatever
  already exists.
- **Full overwrite** — replace everything.

A raw instance-wide `pg_dump` is a poor foundation for this. It embeds the
existing primary/foreign keys of the *whole* database; merging it into an
already-populated instance means either colliding IDs or generically
rewriting arbitrary SQL text, which isn't a safe or reliable operation.
Full-overwrite restore is achievable on top of it (as documented in
ADR-0055), but additive isn't, without effectively building a second,
parallel import path anyway.

The owner has also decided to decouple this feature from real
disaster-recovery database backups: they'll run raw `pg_dump`/host-level
backups themselves, separately, going forward. That removes the one hard
requirement that justified the SQL-dump format in ADR-0055 (byte-for-byte
transferability of the *entire* instance, independent of the app's own
domain model), and opens up a format built for the two import modes instead.

`interchange.export.ExportService` (FR-22, ADR unnumbered at the time)
already produces a comprehensive, self-describing per-world JSON bundle —
every bounded context's data for that world, flat, each child carrying its
parent id, versioned (`exportVersion`) — for Obsidian interop. It has no
import counterpart, and doesn't bundle the world's media files (ADR-0055
called this out explicitly as the reason it couldn't serve as the backup
format at the time).

## Decision

### Format: structured bundle, not SQL
`/api/backup` is re-pointed at a new format — a ZIP of:
- `manifest.json` — `{ "exportVersion": <int>, "exportedAt": <ISO instant>, "worldIds": [...] }`
- `worlds/<worldId>.json` — one `ExportService`-shaped bundle per world (same
  shape already produced for `GET /worlds/{id}/export`, one file per world
  in the instance instead of a single-world download)
- `media/<worldId>/<mediaId>` — the media bytes for that world, added new:
  `MediaLookupPort.findByWorld(worldId)` gives the file list, actual bytes
  come from the existing `MediaStorage` abstraction (ADR-0007)

No `pg_dump`/`psql`, no `postgresql-client` in the runtime image, no shelling
out. `ProcessPgDumpRunner`, `PgDumpRunner`, and `BackupService`'s ZIP-of-SQL
logic are removed; `BackupService` is rewritten against the export/import
ports below instead of a subprocess.

### Import: two modes, one code path
New `POST /api/backup/import?mode=additive|overwrite`, multipart ZIP upload.

Both modes funnel through the same **import-as-new-world** routine: read a
`worlds/<id>.json` bundle, walk it in FK-dependency order (world →
categories/articles → maps → pins → timelines → events → calendars →
relationships → campaigns → sessions/arcs/beats → field templates →
character sheets → statblocks → whiteboards; media alongside), and for
*every* entity generate a fresh UUID, recording old-id → new-id in an
in-memory map so every reference (parent ids, cross-links) gets rewritten
consistently as it goes. Media files are copied under their new media ids.

- **Additive**: run the routine once per world in the bundle. Nothing
  existing is touched, matched, or updated — this always produces brand-new
  worlds, per the owner's explicit choice this session (not
  match-and-merge, which is a much harder, ambiguous problem deferred
  indefinitely).
- **Overwrite**: call the existing `DeleteWorldUseCase` for every
  currently-existing world (already handles cascade cleanup, including
  media files off disk — no new deletion logic needed), then run the same
  additive routine for every world in the bundle.

Always generating fresh ids (even in overwrite mode, where the database is
empty and collisions can't happen) is deliberate: one code path, one set of
edge cases, instead of two subtly different import routines.

### Cross-context wiring
Each bounded context that owns world-scoped data (`worldbuilding`,
`campaign`, `characters`, `whiteboard`, `media`) gets one new published
port/use-case for bulk import-with-explicit-id (the DTO shape mirrors what
`ExportService` already reads via that context's `*QueryPort`). `interchange`
calls only through these published ports, same as `ExportService` does today
— keeps `contextsOnlyUsePublishedPorts` (ArchUnit, ADR-0050) satisfied. Each
new port is a thin "persist this already-validated row with this id"
operation; no new business rules, since validation happened when the data
was first created (by whatever instance originally exported it).

Each world's import runs in one `@Transactional` boundary: a failure partway
(malformed bundle, version mismatch) rolls back that world only; other
worlds in the same request/bundle are unaffected.

### Safety
- `manifest.json`'s `exportVersion` must equal the running
  `ExportService.EXPORT_VERSION`. A mismatch is rejected outright — no
  best-effort partial import of a shape the code wasn't written against.
- Overwrite is destructive (deletes all current worlds first): the frontend
  requires a typed confirmation (not just a click) before submitting
  `mode=overwrite`.

### Frontend
Worlds page: the existing "⬇ Backup" button stays (now downloading the new
format); a new "Import" control adds a file picker plus an additive/overwrite
choice, posting multipart to the new endpoint.

## Consequences
- `pg_dump`/`postgresql-client` is fully removed from the app and the
  combined image — real database backups are the operator's own
  responsibility from here (host-level `pg_dump`, cron, `docker exec`,
  whatever they already use elsewhere). The app no longer claims to produce
  a disaster-recovery-grade instance backup.
- A meaningful new cross-context surface: one import port per bounded
  context that owns world-scoped data. More files, but each one is
  intentionally minimal by design (persist-with-given-id, nothing more).
- Import is only as complete as `ExportService` — anything it doesn't
  capture (revision history, audit trails) doesn't round-trip. Accepted:
  that's edit history *about* the world, not the world's content.
- No merge/conflict resolution. Importing "the same world" twice (e.g.
  re-importing your own last backup, additive) produces two separate worlds
  by design — there's no attempt to detect or dedupe that.
- Old backups (`database.sql` + `media/` ZIPs taken under ADR-0055) are not
  importable by the new endpoint — different format entirely. They're still
  restorable via ADR-0055's manual `psql` procedure if ever needed.

## Alternatives considered
- **Keep the pg_dump format; add `--clean --if-exists` for overwrite,
  defer additive** — would deliver overwrite quickly, but the owner
  explicitly wants both modes now, and additive fundamentally doesn't fit a
  raw instance-wide SQL dump (see Context). Rejected as a dead end for half
  the ask, not just a smaller first step.
- **Restore-via-staging-database**: restore the full pg_dump into a scratch
  Postgres database, then copy world-by-world into the live one with
  ID remapping done in SQL. Rejected — still needs `psql`/a second database,
  and the remapping logic ends up needing the same per-table knowledge the
  JSON approach needs anyway, minus the transactional/validation safety of
  going through the app's own domain layer.
- **Match-and-merge additive semantics** (match imported records to existing
  ones, e.g. by name, and update): rejected for now — genuinely ambiguous
  (what constitutes a match? what wins on conflict?) and out of proportion
  to the stated need ("add as new," not "reconcile two copies of the same
  world").
