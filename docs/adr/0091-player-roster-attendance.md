# 0091. Player roster and per-session attendance

## Status
Accepted

## Context
Character death/replacement (common in systems like Pirate Borg) plus
recurring guest players means there's currently no way to answer "who was
actually at the table when X happened," and no reusable concept of "the
people I play with" across campaigns/oneshots (issue #65).

Requirements from the issue:
- Players are a reusable pool (name only), not recreated per campaign.
- Each campaign has its own roster: a selection of players added to that
  campaign, each flagged regular or guest. Guest is a per-membership flag,
  not a separate list.
- A player can accumulate multiple character sheets over a campaign's life.
- Attendance is per session, per player: `(present: bool, characterPlayedThatSession: optional link)`
  — deliberately not a "current character" pointer on the player, so
  historical attendance stays accurate regardless of later character
  changes.
- The session view gets an Attended | Player | Character table,
  pre-populated from the roster, defaulting to everyone present.
- Surfaces in the session recap (not GM-only).

## Decision

### Scope of "reusable pool": world-scoped, not truly global
Every top-level aggregate in this app is scoped by `worldId` (ADR-0005:
single-user, no tenancy beyond the world boundary) — there is no
precedent anywhere for "global across all worlds," and no need for one.
`FieldTemplate` is the existing precedent for "world-scoped, reused across
every campaign in that world" (no `campaignId` at all). `Player` follows
the same shape: `Player(id, worldId, name, createdAt, updatedAt)`, its own
full aggregate with `/api/worlds/{worldId}/players` CRUD.

### Bounded context: everything stays inside `campaign`
Unlike `tagging` (extracted into its own context because two *unrelated*
contexts — `worldbuilding` and `characters` — both needed it, ADR-0083),
players and attendance are consumed only by `Campaign` and `Session`,
which already live in `campaign`. No new bounded context.

### Package placement mirrors each concept's URL nesting, not a uniform rule
This codebase's existing convention (confirmed via `Beat`, nested inside
`arc`'s own package because its URL nests under `/arcs/{arcId}/beats`; and
`CheatSheet`, nested inside `session`'s own package because its URL nests
under `/sessions/{sessionId}/cheat-sheet`) is: **a child concept's Java
package follows its URL parent, not its conceptual "type."** Applied here:

- **`Player`** — new top-level aggregate slice `domain/player`,
  `application/player`, `adapter/player` (own URL:
  `/worlds/{worldId}/players`).
- **`CampaignPlayer`** (the roster membership row: `playerId` + `guest`
  flag) — lives inside the *existing* `campaign` package
  (`domain/campaign`, `application/campaign`, `adapter/campaign`),
  alongside `Campaign` itself, because its URL nests under campaigns:
  `/worlds/{worldId}/campaigns/{campaignId}/roster`. Not a new package.
- **`SessionAttendance`** — lives inside the *existing* `session` package,
  alongside `Session` and `CheatSheet`, because its URL nests under
  sessions: `/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/attendance`.
  It mirrors `CheatSheet`'s shape specifically (whole-set GET/PUT scoped
  to one session), not `LooseThread`'s shape (individually-addressable
  per-row CRUD with its own top-level package) — attendance rows have no
  standalone identity a client ever addresses directly.

### Whole-set replace, not per-row CRUD
Both the roster and attendance are edited as a **whole set per PUT**
(`GET` returns the current set, `PUT` replaces it entirely), mirroring
`TaggingService.set(...)`'s delete-then-recreate-in-one-transaction shape
and `CheatSheetService.put(...)`'s upsert-the-whole-object shape — not
`LooseThread`'s per-row CRUD. This directly matches how the issue frames
edits ("promoting a guest is just flipping the flag," "the GM unchecks
absences") as bulk state, not row-by-row management.

- **Roster** (`GET`/`PUT /campaigns/{campaignId}/roster`): body is a flat
  list of `{playerId, guest}`. `PUT` deletes all existing
  `campaign_players` rows for the campaign and recreates them from the
  submitted set (each `playerId` validated to exist in the world via a
  new `PlayerExistsPort` ACL).
- **Attendance** (`GET`/`PUT /sessions/{sessionId}/attendance`): `GET`
  unions persisted `session_attendance` rows with the campaign's current
  roster, in both directions — a roster player with no row yet gets a
  synthesized `present=true, characterId=null` entry (matching
  "pre-populated... defaulting to everyone present," and `CheatSheet`'s
  "unsaved sentinel" idea applied per-row instead of to the whole
  object); a saved row for a player no longer on the roster still
  appears too (missing only the guest flag the roster would have
  supplied, which defaults to `false`) — otherwise a roster edit would
  silently hide exactly the history this feature exists to keep. `PUT`
  deletes and recreates the whole set for that session from
  `{playerId, present, characterId}` triples.

### Historical accuracy: attendance outlives roster membership
`SessionAttendance.playerId` is a direct FK to `players(id)`, **not** to
`campaign_players` — removing a player from a campaign's roster (a normal
whole-set roster `PUT` that omits them) does not touch or cascade-delete
their past attendance rows. Only deleting the `Player` aggregate itself
(a real, deliberate action) cascades away their attendance history too.
This is what keeps "who was at the table" accurate even after someone
stops playing.

### Character link: references `characters.CharacterSheet`, not `Statblock`
A "character sheet" in this app is a filled instance of a
`kind=CHARACTER` `FieldTemplate` — the domain class
`characters.domain.sheet.CharacterSheet` (ADR-0031: world-scoped, with a
*nullable* `campaignId` meaning "shared across the world's campaigns").
`SessionAttendance.characterId` is validated, when non-null, via a new
cross-context ACL — `campaign.application.session.port.out.CharacterSheetExistsPort`,
implemented by an adapter composing `characters`' published
`CharacterSheetQueryPort.findByIdInWorld(...)` — to exist in the world
**and** have `campaignId` either null (shared) or equal to this session's
campaign. No `playerId` is added to `CharacterSheet`: the issue explicitly
rejects a "current character" pointer, and the link only ever lives on
the attendance row, so death/replacement never rewrites history.

### Recap: frontend-only change
ADR-0068 established the recap (`RecapView.tsx`) as a deliberately
zero-backend-surface, client-side assembly over existing read endpoints —
GM notes are excluded structurally (never fetched), not filtered.
Attendance is not GM-only, so "surface it in the recap" is exactly one
more `fetch` (roster + attendance per session) and one more rendered
section, with no new backend schema.

## Consequences
- Three new tables: `players`, `campaign_players`
  (`UNIQUE(campaign_id, player_id)`), `session_attendance`
  (`UNIQUE(session_id, player_id)`) — one migration,
  `V37__players_and_attendance.sql`.
- `campaign_players.player_id` and `session_attendance.player_id` are
  `ON DELETE CASCADE` from `players(id)` — deleting a player removes them
  from every roster and erases their attendance history everywhere. This
  is the one sharp edge of the design; acceptable for a single-user tool
  where "delete" is a deliberate, infrequent action.
- `session_attendance.character_id` is `ON DELETE SET NULL` from
  `character_sheets(id)` — a deleted character sheet just clears the
  attendance row's link, it never deletes attendance history.
- Every new aggregate gets the standard published query/import port pair
  (`PlayerQueryPort`/`PlayerImportPort`,
  `CampaignPlayerQueryPort`/`CampaignPlayerImportPort`,
  `SessionAttendanceQueryPort`/`SessionAttendanceImportPort`) so world
  export/import (ADR-0061) covers them like every other aggregate — no
  exception carved out.
- `docs/api/openapi.yaml` gains `Player`/`PlayerRequest`,
  `RosterEntry`/`RosterRequest`, `AttendanceEntry`/`AttendanceRequest`
  schemas and three path groups, following the `Session`/`CheatSheet`/
  `EntityTags` conventions already there.

## Alternatives considered
- **Per-row CRUD for roster/attendance** (like `LooseThread`). Rejected:
  the issue frames both as bulk toggles ("GM unchecks absences"), and a
  whole-set replace is simpler on both ends for that interaction shape.
- **A `currentCharacterId` pointer on `Player` or `CampaignPlayer`
  instead of per-session links.** Rejected — the issue explicitly calls
  this out as wrong: it would silently rewrite history on every
  character death/replacement.
- **Filtering the character dropdown to only that player's own
  sheets** (would require a `playerId` on `CharacterSheet`). Rejected:
  the issue never asks for sheet ownership, only for the played-this-
  session link to exist; adding sheet ownership is a bigger, unrequested
  change to the `characters` context. The GM picks manually from the
  campaign's visible sheets instead.
- **A backend recap DTO extension.** Rejected in favor of staying
  consistent with ADR-0068's established zero-backend-surface recap —
  inventing a backend recap schema now would contradict that precedent
  for no benefit.
