# ADR-0026: Article revision history

- Status: Accepted
- Date: 2026-08-12

## Context
FR-21 wants article edit history so a worldbuilder can review or undo changes.
Only articles carry substantial prose worth versioning; other entities are
small and structured.

## Decision
Keep an **append-only snapshot** of an article's content each time it changes.
An `article_revisions` row stores the article's `title`, `slug`, `template`, and
`body` **as they were before** an update, plus a `created_at`. Snapshots are
taken on **update** (and before a **restore**), not on create.

Endpoints:
- `GET /worlds/{worldId}/articles/{articleId}/revisions` — list newest first.
- `POST /worlds/{worldId}/articles/{articleId}/revisions/{revisionId}/restore` —
  snapshot the current state (so the restore itself is undoable), then copy the
  revision's content back onto the article.

Revisions cascade-delete with their article. Bodies stored in revisions are
already sanitized (ADR-0025), since they are copies of the live article body.

## Consequences
- Full, linear history with one-click restore; restores are themselves undoable.
- Storage grows with edits; acceptable for a single user. A retention cap (keep
  last N) can be added later without an API change.
- Slugs are snapshotted, so restoring old content restores its old slug too;
  uniqueness still holds because a restore reuses the article's own past slug.

## Alternatives considered
- **Diff storage**: smaller but complex to reconstruct and show; full snapshots
  are trivial to restore and display at this scale.
- **Versioning every entity**: unnecessary for small structured records.
