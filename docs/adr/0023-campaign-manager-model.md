# ADR-0023: GM campaign manager model

- Status: Accepted
- Date: 2026-08-12

## Context
Phase 3 adds the gamemaster tools (FR-12…FR-15): campaigns that group play
sessions and plot structure, tied to a world's lore. This is the GM's running
record, distinct from the world's encyclopaedic articles.

## Decision
Four world-scoped concepts, mirroring the maps/timelines pattern:

- **Campaign** (`campaigns`) — a run of play within a world: name, description,
  free-text GM notes. A world may have several campaigns.
- **Session** (`sessions`) — a single game session under a campaign: title,
  an integer `session_number` for ordering, an optional real `date` (a plain
  `DATE`, since sessions happen on real-world days — unrelated to fantasy
  calendars, ADR-0020), a summary, and private GM notes. Sessions list in
  `session_number` then `date` order.
- **Arc** (`arcs`) — a plot thread under a campaign: title, description, and a
  `status` enum (`PLANNED`, `ACTIVE`, `COMPLETED`, `ABANDONED`), ordered by a
  `position`.
- **Beat** (`arc_beats`) — an ordered story beat within an arc: title, optional
  body, a `done` flag, and an optional link to an **article** (the NPC/location
  it concerns) and/or the **session** where it happened. Ordered by `position`.

Cross-links (`article_id`, `session_id`) are nullable and `ON DELETE SET NULL`,
so deleting lore or a session never destroys plot structure. Everything cascades
from its parent (world → campaign → session/arc → beat).

Ordering fields (`session_number`, `position`) are caller-supplied integers, as
with calendar months (ADR-0020); the API does not renumber siblings.

## Consequences
- The GM's play record is cleanly separated from world lore but can reference it.
- Real session dates use a plain `DATE`, sidestepping the fantasy-calendar model.
- Deleting an article leaves a beat intact (link nulled), matching how map pins
  and timeline events already behave.
- Renumbering/reordering is the client's job; simple and race-free at single-user
  scale.

## Alternatives considered
- **Reusing timelines for sessions**: sessions are real-world dated and carry GM
  notes and arc structure; conflating them with in-world timelines would muddy
  both.
- **Arcs as world-level (not campaign) entities**: plot threads belong to a run
  of play, so campaign-scoping is the better fit.
