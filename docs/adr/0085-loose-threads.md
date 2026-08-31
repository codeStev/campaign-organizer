# ADR-0085: Loose threads

- Status: Accepted
- Date: 2026-09-01

## Context
GMs often improvise a throwaway detail at the table that players
unexpectedly latch onto — "something happened, and future-you needs to
decide what to do with it." There's currently nowhere to jot this down that
isn't a fully planned beat. Loose threads are **retrospective** (logged
after they already happened), the opposite of beats (**prospective**,
planned toward). Per the issue: quick-add lives right in the session's own
edit view alongside GM notes, with zero extra navigation during the
after-session write-up pass; three-state status (open/resolved/abandoned —
abandoned keeps the list from accumulating dead weight); no forced
structural link to beats or articles (the real workflow when a thread
matters is just creating a normal article). It will surface on a future
per-campaign dashboard (#67, not yet built), but deliberately not in
ArcBoard.

## Decision
- **A true child aggregate, not a JSONB blob on `Session`.** `CheatSheet`
  (ADR-0071) is the nearby precedent for "session data, structured" living
  as a whole-replace JSONB field — but that pattern fits fragments that are
  *always* loaded/saved together with no independent lifecycle. Loose
  threads are the opposite: a status flips independently after creation, a
  thread gets deleted on its own, and a new one gets quick-added without
  touching anything else on the session. That's exactly what a child table
  with its own id and its own CRUD is for.
- **Lives inside the existing `campaign` bounded context**, sibling to
  `session`/`arc`/`clock` — same context as its parent, so no
  anti-corruption adapter is needed to check the parent session exists.
- **A denormalized `campaign_id` column, not just `session_id`.**
  `ArcBeat` is the cautionary counter-example: it only carries `arc_id`, so
  today's frontend has to fetch every arc in a campaign and union their
  beats client-side (`frontend/src/lib/beats.ts`) to answer "all beats in
  this campaign." Clocks avoided that by putting `campaign_id` directly on
  the `clocks` table, which is exactly why `ClockQueryPort.findByCampaign`
  is a single indexed query. Loose threads take the same shape: `session_id`
  for the session-scoped view (list/quick-add), `campaign_id` alongside it
  so a future dashboard's "all open loose threads for campaign X" is one
  query on `(campaign_id, status)`, not a fan-out through every session.
- **Whole-object PUT on update** (`{text, status}` together) — matches every
  other aggregate in this codebase; there's no PATCH endpoint anywhere. A
  status-only change from the UI just resends the current text.
- **Immediate persistence, no Cancel.** Loose threads follow `ClockBoard`'s
  interaction style (ADR-0084): add/status-change/delete each fire their own
  request right away with a toast, independent of `SessionLog`'s own
  read/edit-split-with-Save-button flow for the session's title/summary/
  notes. A thread the GM types during write-up shouldn't be lost because
  they forgot to also hit "Save session."
- **`LooseThreadQueryPort.findByCampaign(campaignId)`** is the port a future
  dashboard (#67) will consume for the "open loose threads" widget — this
  ADR only ensures the port and its `(campaign_id, status)` index exist; no
  dashboard work happens here.
- **Backup/export/import parity** (FR-36): a `LooseThreadImportPort` mirrors
  `ArcImportPort`/`ClockImportPort`, wired into `ExportService`/
  `ImportService` with both `sessionId` and `campaignId` remapped on import.

## Consequences
- Adding loose threads costs one small aggregate with its own table; no new
  bounded context, no ArchUnit registration change.
- The denormalized `campaign_id` must stay in sync with the parent
  session's campaign — acceptable because a loose thread is never moved to
  a different session after creation (not asked for, not exposed in the
  UI), so the value is set once at creation and never needs reconciling.
- Loose threads deliberately have no link to beats/articles/anything else —
  if a thread earns a real article later, that's an ordinary new article,
  not a conversion feature this ADR needs to support.

## Alternatives considered
- **JSONB field on `Session`** (mirroring `CheatSheet`): rejected — status
  changes and deletions are independent per-thread actions, which a
  whole-blob-replace field handles awkwardly (every status flip would
  resave the entire list).
- **`session_id` only, no `campaign_id`**: simpler schema, but reproduces
  `arc_beats`' exact multi-hop problem for the one query (#67's dashboard)
  this feature explicitly exists to eventually support.
- **Surfacing loose threads in `ArcBoard`**: the issue explicitly rules this
  out for v1 — threads aren't tied to any arc, and the dashboard (#67) is
  the intended surface once it exists.
