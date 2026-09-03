# 0101. Beat kinds

## Status
Accepted

## Context
Reviewing a Claude Design mockup for the ongoing UI overhaul
(`docs/ui-overhaul-plan.md`), beats in the mockup carry a small colored tag
("Combat", "Reveal", "Downtime"...) that makes an arc's beat list scannable
at a glance. The user confirmed wanting this, explicitly not as a fixed
enum (SCENE/COMBAT/REVEAL...) but as GM-defined vocabulary, "same shape as
Game Systems' color field" (name + color, small CRUD, own table).

`Beat` (`ArcBeat` in code) already lives in the `campaign` bounded context
alongside `Arc`, `Player`, `Encounter`, etc. — a story beat only ever
belongs to one campaign, which belongs to one world.

## Decision
A new `BeatKind` aggregate — **world-scoped**, not global like
`GameSystem`. Beat vocabulary ("Heist", "Ritual", "Chase") is narrative
flavor tied to a specific setting/genre, unlike a game system which is
genuinely reused unchanged across many worlds (ADR-0094's rationale for
going global). Two GMs running different genres in different worlds
should get independent kind catalogs, not one that grows cluttered with
every kind either has ever used.

- **Aggregate**: `campaign.domain.beatkind.BeatKind` — `id, worldId, name,
  color, createdAt, updatedAt`. Same shape/validation as `Player`
  (world-scoped sibling in the same context) with `GameSystem`'s `color`
  field bolted on; no `tagline`/`notes` — beat kinds don't need them.
- **Schema** (`V49__beat_kinds.sql`): `beat_kinds` table (`world_id NOT
  NULL REFERENCES worlds(id) ON DELETE CASCADE`, matching `players`), a
  per-world case-insensitive unique name index (matching `game_systems`'
  global one, scoped down — same typo-fragmentation concern ADR-0095
  raised for game systems applies here too), and a nullable `kind_id` on
  `arc_beats` (`REFERENCES beat_kinds(id) ON DELETE SET NULL`) — same
  "informational, not structurally load-bearing" treatment as
  `campaigns.system_id` (ADR-0095): deleting a kind un-tags its beats
  instead of being blocked or cascading.
- **API**: `/api/worlds/{worldId}/beat-kinds` (world-scoped CRUD, mirrors
  `PlayerController`'s shape) rather than `/api/game-systems`' flat
  top-level path.
- **Beat integration**: `BeatRequest`/`Beat` gain an optional `kindId`.
  `ArcBeatCommandService` validates it via a directly-injected
  `BeatKindQueryPort` — no anti-corruption adapter, since `BeatKind` is a
  sibling aggregate in the *same* context (same pattern as the existing
  `EncounterQueryPort` dependency there), not a cross-context reference.
- **Frontend**: beats render with their kind's color the way the mockup
  shows (resolved client-side from a separately-fetched kind list, the
  same pattern `Campaign.systemId` already uses for `GameSystem` — the
  API never embeds a resolved name/color, only the raw id).
- **Backup/export**: included in the per-world bundle
  (`ExportService`/`ImportService`), world-scoped and remapped exactly
  like `players` — no resolve-or-reuse merge logic, since import always
  lands in a freshly-created world.

## Consequences
- A new full hex-arch slice (domain/application/adapter rings, its own
  ADR-mandated Flyway migration) — the smallest of the app's "small user
  catalog" aggregates (`Player`, `GameSystem`, now `BeatKind`), reusing
  `Player`'s exact shape as the closest precedent.
- `arc_beats` gains one nullable column; no existing beat behavior
  changes when `kindId` is absent.
- Per-world uniqueness means the same kind name ("Combat") can exist
  independently in different worlds without collision — intentional,
  unlike `GameSystem` where that would be duplication.

## Alternatives considered
- **Global catalog, like `GameSystem`** — rejected: beat vocabulary is
  narrative flavor specific to a setting/genre, not a reusable rules
  reference; a global list would accumulate one-off kinds from every
  world the user has ever run.
- **Fixed enum** (`SCENE`/`COMBAT`/`REVEAL`/...) — rejected per the user's
  explicit decision; GMs' campaigns don't share a single useful
  vocabulary, and a closed set forces awkward-fit choices the way a
  free-text field or per-world catalog doesn't.
- **Campaign-scoped instead of world-scoped** — considered, since a beat's
  campaign is already knowable from its arc; rejected as unnecessarily
  narrow — a GM running several campaigns in one world/setting (e.g. a
  long-running West Marches world) would want to reuse the same kind
  vocabulary across them, the same way `Player`'s world-scoped pool is
  reused across a world's campaigns rather than duplicated per campaign.
