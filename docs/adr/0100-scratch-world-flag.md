# 0100. Scratch/sandbox world flag

## Status
Accepted

## Context
Reviewing a Claude Design mockup for the ongoing UI overhaul
(`docs/ui-overhaul-plan.md`), its world switcher listed a "Sketchbook ·
scratch" world alongside real campaign worlds — a lightweight way to mark
a world as a brainstorming/sandbox space, distinct from an actual campaign
setting, without needing separate tooling. The user confirmed wanting this,
scoped deliberately small: cosmetic only, no functional exclusion from
search, backup, or export.

## Decision
`World` gains a `scratch: boolean` (default `false`), plain and
non-functional:
- **Schema**: `ALTER TABLE worlds ADD COLUMN is_scratch BOOLEAN NOT NULL
  DEFAULT FALSE` (`V48__world_scratch_flag.sql`) — additive, no backfill
  needed beyond the default.
- **Domain**: a field on `World` (`worldbuilding` context), set via the
  existing `create`/`update` methods alongside name/description — not a
  separate use case, since it's just another basic attribute of a world,
  unlike `layerStyles` (its own sub-resource with its own replace
  endpoint).
- **No functional effect anywhere else.** Explicitly rejected: excluding
  scratch worlds from global search, instance backup, or export. A scratch
  world is a full, real world in every functional sense — the flag is
  purely a label.
- **Frontend**: a checkbox on world creation, shown as a small badge/label
  in the world list and the `/next` world switcher panel. No other UI
  reads it.

## Consequences
- `WorldRequest`/`World` (OpenAPI), `WorldCommands`, `WorldView`,
  `WorldJpaEntity` all gain the field in lockstep — one flag, no new
  aggregate, no new port.
- Existing worlds default to `scratch = false` (real worlds), matching
  current behavior exactly for anyone not using the new flag.

## Alternatives considered
- **Functional exclusion (search/backup/export)** — rejected per explicit
  user direction: cosmetic only, simplest version, matches the mockup's
  own "Sketchbook" entry which behaves identically to any other world.
- **A richer `WorldKind` enum** (e.g. `CAMPAIGN` / `SCRATCH` / `ARCHIVED`)
  instead of a boolean — rejected as speculative; nothing today calls for
  more than two states, and a boolean is trivially extensible later if a
  real third state emerges.
