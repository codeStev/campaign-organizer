# 0102. World overview aggregate stats

## Status
Accepted

## Context
The mockup's world Overview screen (`docs/ui-overhaul-plan.md` Phase 4)
opens on a stats strip — article count, sessions run, a recently-edited
feed — before any of Phase 4's UI work can consume it, the read endpoint
needs to exist. Per the Decisions section, the word-count stat was
explicitly dropped (on-read cost grows with world size; not worth a
cached/denormalized counter for one stat), leaving three genuinely cheap
aggregates, all derivable from data that already exists.

## Decision
A pure read-composition endpoint, `GET /api/worlds/{worldId}/overview`,
following the exact shape of the existing consistency report
(`ConsistencyReportService`, FR-43): no new aggregate, no persistence, no
domain model — a `@Service` in a new `interchange.overview` sub-context
that composes existing published query ports and returns a plain read
record straight from the controller (matching `ConsistencyReportController`
— for a stateless query composition with nothing to protect, there's no
separate domain/persistence/web trio to keep apart).

- **Article count** — `ArticleQueryPort.findByWorld(worldId).size()`.
- **Sessions-run count** — sessions (across every campaign in the world)
  whose `date` is on or before today (`Clock`-based, not
  `LocalDate.now()` directly, per this repo's testability convention).
  A session with no date yet, or a future date, doesn't count as "run" —
  matches the print-first workflow's session lifecycle (a session is
  scheduled, then played, then documented afterward).
- **Recently-edited feed** — the 5 most recently updated articles
  (`updatedAt` desc), id/title/timestamp only. Scoped to articles only,
  not a cross-context activity feed: the plan's own phrasing ("article
  `updatedAt`") and the "no new persisted state" constraint both point at
  articles as the intended source — a true multi-context feed would need
  either new persisted state or an expensive fan-out merge-sort across
  every context on every request, and nothing has asked for that yet.

## Consequences
- No Flyway migration — this ADR exists only because it's a new backend
  read capability per Ground Rule 4, not because there's new persisted
  state.
- Consumed by Phase 4's Overview dashboard; nothing today calls this
  endpoint yet.

## Alternatives considered
- **Denormalized/cached counters** (e.g. a `stats` column on `worlds`
  updated on every write) — rejected: premature for numbers this cheap to
  compute on read at this data scale (a personal, single-user app), and
  it would need invalidation logic on every write path that touches
  articles or sessions.
- **Cross-context recently-edited feed** (articles + statblocks + maps +
  …) — rejected for now as scope creep beyond what the plan actually
  asked for; article-only is a straightforward extension point if a
  richer feed is wanted later.
