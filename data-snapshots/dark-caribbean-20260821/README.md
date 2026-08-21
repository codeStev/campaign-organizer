# Data snapshot: Dark Caribbean, 2026-08-21

A point-in-time backup of the real "Dark Caribbean" world, taken after the
Markdown-editor migration (ADR-0054) landed on `master`. Kept on its own
branch (`backup/dark-caribbean-20260821`) so it never lands on `master` or
mixes into feature history — this branch exists only to hold this snapshot.

- `database.dump` — full `pg_dump` of the `campaign_organizer` database,
  custom format (`-Fc`).
- `media/` — every file from the app's media volume (`APP_MEDIA_DIR`),
  flat by media id (matches the `media` table's stored filenames).

## How to import this snapshot

1. **Bring up a fresh stack** (or point at the target Postgres instance) —
   e.g. from a checkout of this repo: `cp .env.example .env && docker compose
   up -d db` and wait for it to report healthy (`docker compose ps`).

2. **Restore the database.** From the repo root, with this branch checked
   out (or after copying `database.dump` next to your compose file):
   ```bash
   docker cp data-snapshots/dark-caribbean-20260821/database.dump \
     campaign_organizer-db-1:/tmp/restore.dump
   docker exec campaign_organizer-db-1 \
     pg_restore -U app -d campaign_organizer --clean --if-exists \
     /tmp/restore.dump
   ```
   `--clean --if-exists` drops existing objects first, so this is safe to
   run against a freshly created, empty database — it will error harmlessly
   on the "doesn't exist yet" drops the first time, which is expected.

3. **Restore the media files.** Start the backend once so its media
   directory/volume exists, then copy the files in:
   ```bash
   docker compose up -d backend
   docker cp data-snapshots/dark-caribbean-20260821/media/. \
     campaign_organizer-backend-1:/data/media/
   ```

4. **Bring up the rest of the stack** (`docker compose up -d`) and verify:
   log in, confirm the "Dark Caribbean" world lists 17 articles, and that a
   couple of articles with embedded images render correctly.

This is a manual snapshot taken ahead of the one-click backup feature
(ADR-0055, in progress on `feature/instance-backup`) — once that ships, the
app can produce an equivalent bundle itself via a single click instead of
this by-hand `docker exec` procedure.
