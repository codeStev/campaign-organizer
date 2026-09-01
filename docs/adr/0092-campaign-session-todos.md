# 0092. Campaign and session todos

## Status
Accepted

## Context
There's no lightweight place to track GM task-list items — "print handout
X," "reskin the goblin statblock," "update faction standings" — distinct
from loose threads (retrospective narrative content, ADR-0085) and beats
(prospective story structure, ADR-0023). Two kinds (issue #66):
- **Campaign-level**: standing chores, not tied to any session, shown on
  the future dashboard (#67) until done.
- **Session-level**: attached to a specific session; its "due date" is
  just that session's date, no separate due-date field. Surfaces while
  prepping that session, and on the dashboard once it's "next."

Shape is deliberately minimal: text + done checkbox, no priority, no
manual ordering beyond creation.

## Decision
One aggregate, `Todo(id, campaignId, sessionId, text, done, createdAt,
updatedAt)`, with `sessionId` nullable: null means a standing
campaign-level todo, non-null means it's attached to that session. This
mirrors `CharacterSheet`'s nullable-`campaignId` idiom (ADR-0031) applied
one level down — a single aggregate with an optional narrower scope,
rather than two separate types.

**Own top-level aggregate slice** (`domain/todo`, `application/todo`,
`adapter/todo`), matching `LooseThread`'s placement rather than nesting
inside `session` or `campaign` — like loose threads, todos are
individually-addressable, per-row CRUD (not a whole-set replace like the
roster/attendance from ADR-0091), so they get their own package the same
way `LooseThread` does despite conceptually hanging off a session.

**Two list/create surfaces, one shared update/delete route:**
- `GET`/`POST /worlds/{worldId}/campaigns/{campaignId}/todos` — standing
  todos only (`sessionId` is always null through this route; create here
  can never attach a session).
- `GET`/`POST /worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/todos` —
  that session's todos only (create here always attaches `sessionId`).
- `PUT`/`DELETE /worlds/{worldId}/campaigns/{campaignId}/todos/{todoId}` —
  shared by both kinds, nested only under the campaign (not the session),
  since `todoId` is already unique and a todo never moves between
  standing and session-attached after creation. This keeps editing a
  session todo simple for any caller that only knows the campaign (e.g.
  a future dashboard) without needing to also know which session it
  belongs to.

Persistence: one table `todos`, `session_id` nullable
`REFERENCES sessions(id) ON DELETE CASCADE` (a deleted session takes its
todos with it — they have no meaning once the session is gone),
`campaign_id NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE`.
Migration `V38__todos.sql`.

Ports/service mirror `LooseThread` exactly (`CampaignExistsPort`/
`SessionExistsPort` ACL adapters, `Todo`/`TodoStatus`-free boolean
`done` instead of a status enum since there's no third state to model),
plus the standard published `TodoQueryPort`/`TodoImportPort` pair for
world export/import (ADR-0061).

## Consequences
- No dashboard surfacing yet — issue #67 (per-campaign dashboard) is a
  separate, not-yet-built issue; this ADR only covers the data model and
  the two places it's visible today: the campaign view (standing todos)
  and the session edit/read view (that session's todos). When #67 lands,
  it consumes the same `TodoQueryPort.findByCampaign`/`findBySession`
  this ADR establishes — no rework expected.
- No priority or manual ordering: list order is creation order
  (`createdAt`), matching the issue's explicit "no priority, no manual
  ordering" requirement.

## Alternatives considered
- **Two separate aggregates** (`CampaignTodo`, `SessionTodo`). Rejected:
  the two kinds share every field and behavior except the presence of
  `sessionId` — a nullable field is simpler than two near-duplicate
  aggregates, ports, and controllers.
- **A status enum** (like `LooseThreadStatus`). Rejected: the issue
  explicitly asks for a plain checkbox, and there's no third state (no
  "abandoned" todo) — a boolean is the honest model.
