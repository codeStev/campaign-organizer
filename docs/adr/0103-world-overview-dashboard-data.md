# 0103. World overview dashboard data

## Status
Accepted

## Context
ADR-0102 shipped a deliberately narrow `GET /worlds/{worldId}/overview`
(article count, sessions-run count, recently-edited feed) ahead of any
consumer, noting "nothing today calls this endpoint yet." Building
Phase 4's actual Overview screen (`docs/ui-overhaul-plan.md`) now surfaces
the rest of what that screen needs: a next-scheduled-session card, a
Clocks widget, and a Loose Threads widget — all cross-campaign summaries
scoped to the world, none of which the existing published query ports
expose (`ClockQueryPort`/`LooseThreadQueryPort`/`SessionQueryPort` are all
per-campaign only, same as `ArcBeatQueryPort` etc.).

Same "no new persisted state" character as ADR-0102 — this is composing
existing per-campaign data across every campaign in the world, not a new
domain concept — but real design choices (what counts as "next", which
clocks/threads surface, how many) warrant recording rather than silently
editing ADR-0102's file, per this repo's "supersede, don't rewrite"
convention.

## Decision
`WorldOverviewStats` (same endpoint, extended shape) gains three more
fields, computed by fanning `WorldOverviewService` out across
`CampaignQueryPort.findByWorld(worldId)` exactly like `ExportService`
and `ConsistencyReportService` already do for sessions/arcs/clocks/etc:

- **`nextSession`** (nullable) — the single nearest session, across every
  campaign in the world, whose `date` is strictly after today (a session
  dated today already counts toward `sessionsRunCount`, so the two never
  double-count the same session). `null` when nothing's scheduled.
  Carries `campaignName` since a world-level view spans campaigns the way
  a per-campaign dashboard wouldn't need to.
- **`openClocks`** — every clock across the world's campaigns that isn't
  yet full (`filledSegments < totalSegments`), sorted most-filled-first
  (closest to completing surfaces first — the same "near filling" prep
  signal the original brainstorm envisioned for a future per-campaign
  dashboard, FR-67, applied here at world scope instead), capped at 8.
  Full clocks are done and not shown.
- **`openLooseThreads`** — every thread across the world's campaigns with
  `status == OPEN` (RESOLVED/ABANDONED excluded), newest-first, capped
  at 8.

Both caps exist only to keep a dashboard widget a widget, not a full
list — same reasoning as `recentlyEdited`'s existing cap of 5.

## Consequences
- One request renders the whole Overview screen — no widget round-trips
  separately, matching this app's existing "one composed response" style
  for read aggregates (`ConsistencyReport`, `WorldExportBundle`).
- `WorldOverviewService` now depends on `ClockQueryPort` and
  `LooseThreadQueryPort` in addition to what ADR-0102 already wired in.
- No FR renumbering — this is the same FR-62 capability, filled in to
  match what its one real consumer (the Overview screen) actually needs.

## Alternatives considered
- **Separate endpoints per widget** (`/overview/next-session`,
  `/overview/clocks`, …) — rejected: more round trips for one screen that
  renders all of it at once, and none of these pieces has an independent
  reason to exist outside the Overview screen the way, say, the
  consistency report does.
- **"Near filling" as a numeric threshold** (e.g. clocks ≥ 75% full) —
  rejected in favor of just showing every open clock sorted by fill: a
  world with few clocks would show an empty widget under a threshold,
  and sorting already puts the most-relevant ones first without a magic
  number to tune later.
