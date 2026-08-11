# ADR-0016: Media storage abstraction and image serving

- Status: Accepted
- Date: 2026-08-11

## Context
The wiki needs image uploads (article art, later maps). ADR-0007 chose local
disk. Two concrete questions remain: how the storage is abstracted, and how
images are served given the API uses bearer-token auth (a browser `<img>` cannot
send an `Authorization` header).

## Decision
- **Storage abstraction.** A `MediaStorage` interface (store / load / delete)
  with a `LocalMediaStorage` implementation writing to `APP_MEDIA_DIR`, one file
  per asset keyed by a random UUID. A future S3 implementation can drop in
  without touching callers.
- **Metadata in Postgres.** A `media` row per asset (world, original filename,
  content type, size, storage key). Deleting a world cascades.
- **Split endpoints by auth need.**
  - Management (`POST`/`GET list`/`DELETE`) under `/worlds/{worldId}/media` is
    **authenticated**.
  - Byte serving is **`GET /media/{id}/content`, public**, addressed by an
    unguessable UUID, so `<img src>` works in article HTML and previews.
- **Only images.** Uploads are restricted to a small allowlist of image content
  types and a size limit; anything else is rejected with `400`.

## Consequences
- Images render anywhere with a plain URL; no per-image token plumbing.
- The public content route is *security-by-obscurity* for a personal, single-user
  app — an acceptable, documented trade-off. If sharing/hardening is ever needed,
  switch to signed URLs (a future ADR).
- Local files and the Postgres dump together form a complete backup.

## Alternatives considered
- **Authenticated image serving + object URLs in JS**: works for previews but not
  for `<img>` embedded in stored HTML; rejected as too limiting.
- **Signed URLs**: more secure but unnecessary complexity for personal use now.
