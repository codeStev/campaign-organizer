# ADR-0019: Timelines model

- Status: Accepted
- Date: 2026-08-11

## Context
Phase 2 adds timelines (FR-9): dated events that can link to articles, with the
ability to keep parallel timelines (e.g. per culture or character). Fantasy
calendars (FR-10) are a separate, later feature — timelines must not depend on
them yet.

## Decision
- **Parallel timelines = multiple `timeline` rows** per world. There is no
  special "parallel" concept; the frontend can display several side by side.
- **Calendar-agnostic numeric dates.** An event stores an integer `year`
  (negative allowed for "before" epochs), plus optional integer `month` and
  `day`. No month-name or month-length assumptions are baked in, so a fantasy
  calendar (ADR to come) can later supply names/lengths without changing stored
  data.
- **Ordering** is by `year`, then `month`, then `day`, with nulls first — so a
  year-only event sorts ahead of dated events in the same year.
- Events may link to an article (`article_id`, nullable, set null on article
  delete). Timelines and events are world-scoped; deletes cascade.

## Consequences
- Timelines ship now, independent of calendars; adding calendars later is
  additive (a timeline may reference a calendar for display only).
- Sorting is deterministic and index-supported.
- Negative years cover pre-epoch history without a special flag.

## Alternatives considered
- **A single timeline per world with a "track" field**: less flexible than
  first-class timelines for parallel tracks.
- **Store a real date/instant**: wrong model for fictional calendars and
  arbitrary year ranges.
