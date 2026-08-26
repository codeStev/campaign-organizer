# 68. Session recap builder (FR-45)

Date: 2026-08-26
Status: Accepted

## Context

Opening a new session means re-telling "the story so far". The data already
exists — session summaries in the session log, story beats with a done flag —
but assembling it by hand every week is busywork. FR-45 asked for one click
that renders the recap as a printable document.

Scope was decided with the owner: the recap contains **past sessions'
summaries and completed beats only**. Session GM notes are private prep and
deliberately excluded; open beats describe the future, not the past, so they
stay out too.

## Decision

A **client-side, read-only assembly** over the existing endpoints — no new
API contract, no schema change:

- A `RecapView` (new window via the ADR-0038 `NewWindowPortal` print
  pattern) is opened from a "🖨 Recap" button on the campaign's Sessions
  panel.
- It fetches the campaign's arcs, all beats per arc, and the sessions list,
  then renders two sections:
  1. **Sessions** in play order (`sessionNumber`, ties broken by date) with
     their rendered Markdown summaries.
  2. **Story so far**: arcs by position; under each, only `done` beats in
     position order, title plus rendered body.
- GM notes are never fetched into the component at all — exclusion is
  structural, not a rendering choice.

## Consequences

- Zero backend surface: nothing to migrate, nothing to re-permission; the
  recap can never drift from what the screens show because it reads the same
  endpoints.
- Ordering relies on client-side sorting of small personal-scale lists;
  fine for this app's single-owner scope.
- If beat bodies later grow spoiler-sensitive sections, the recap's
  content rule ("completed beats") needs revisiting in a new ADR rather
  than silently here.
