# 0094. Game system as a real entity

## Status
Accepted

## Context
ADR-0093 made `system` the sharing key for the global field template
catalog, but it's still a free-text string on both `FieldTemplate` and
`GlobalFieldTemplate` — typed into a cramped, unstyled text input in the
template editor (no `flex` rule, unlike the name field next to it), with
no list to pick from and no protection against "D&D 5e" vs "dnd5e" vs
"D&D 5E" silently fragmenting what should be one system. The user also
wants a home to eventually attach rule references to a system,
independent of any game world — which a free-text label can never be.

## Decision
A new top-level, world-independent aggregate, `GameSystem(id, name,
createdAt, updatedAt)` — deliberately minimal for now (just a name), but
a real entity with stable identity specifically so rule references or
other system-level metadata have somewhere to attach later without
another migration of this shape. Lives in the `characters` context
alongside `GlobalFieldTemplate` (`domain/template/GameSystem.java`),
following the exact same CRUD/port/controller shape ADR-0093 already
established for `GlobalFieldTemplate` (own `port.in`/`port.out`/
`port.published`, own `GameSystemController` at `/api/game-systems`, no
`{worldId}` in the path). `name` is unique (case-insensitive) — enforced
by a functional unique index plus a friendly service-level check before
hitting it, since silent fragmentation-by-typo is exactly what this ADR
exists to prevent.

**`FieldTemplate.system: String` and `GlobalFieldTemplate.system: String`
become `systemId: UUID`** (FK to `game_systems`):
- `FieldTemplate.systemId` stays **nullable** — a one-off world-local
  template still doesn't have to be assigned a system, matching today's
  optional `system` string.
- `GlobalFieldTemplate.systemId` stays **required**, `ON DELETE RESTRICT`
  — a global template always needs a system (unchanged rule from
  ADR-0093, now enforced by FK instead of a non-blank string check), and
  a system still backing a global template can't be deleted out from
  under it, mirroring `global_template_id`'s own RESTRICT policy.
- `FieldTemplate.systemId` is `ON DELETE SET NULL` — deleting a system
  just orphans a world template's reference, it doesn't touch the
  template itself (matches the "optional, informational" spirit already
  established there).

**Migration, same two-step shape as ADR-0093's `V39`/`V40`:**
`V41__game_systems.sql` (schema only: create `game_systems`, add nullable
`system_id` FK columns to both tables) then
`V42__BackfillGameSystems.java` (Flyway Java migration, the second one in
this repo after `V40`): groups every existing `system` string across
*both* `field_templates` and `global_field_templates` by
`trim(lower(system))`, creates one `GameSystem` per distinct value
(display name from the most-recently-updated occurrence's original
casing), sets `system_id` on every matching row in both tables, then
tightens `global_field_templates.system_id` to `NOT NULL` and drops both
tables' now-redundant `system` string columns. One backfill covering both
tables (not two separate ones) — a "homebrew" `FieldTemplate` and a
"homebrew" `GlobalFieldTemplate` should resolve to the *same* `GameSystem`
row, not two.

**`GlobalFieldTemplateImportPort.importOrReuse`'s matching key changes**
from `(kind, system, name)` to `(kind, systemId, name)` — which means a
`GameSystem` must be resolved (via its own new `GameSystemImportPort.
importOrReuse`, same resolve-by-name-not-remap treatment as
`GlobalFieldTemplate` gets in ADR-0093, for the same reason) *before* the
global template it belongs to, threaded through `ImportService`'s local
resolution map alongside the existing global-template-id one.

**Promotion (ADR-0093's "promote to global") now requires the source
world template to already have a `systemId`** — promoting a template
with no system assigned fails with a clear validation error ("assign a
game system before promoting") rather than silently promoting into a
systemless state that `GlobalFieldTemplate` doesn't allow.

**Frontend**: `TemplateBuilder.tsx`'s system field becomes a `Select`
sourced from `gameSystemsApi().list()`, with a small "+ New system"
action (a `PromptDialog`, matching this codebase's existing inline-create
pattern) instead of free text — this is also the fix for the reported
sizing bug, since a proper `Select` gets the same `flex` treatment the
name input already has, rather than an unstyled, unclassed `Input`. A
minimal "Game Systems" list (rename/delete) is added as a section on the
existing global-templates page (`/templates/global`) rather than a new
top-level nav entry — it's reached from exactly the place a GM is already
standing when they'd want to manage systems, and "top-level" here means
*not world-scoped*, which that page already is.

## Consequences
- Every place that read/wrote `FieldTemplate.system`/
  `GlobalFieldTemplate.system` as a string (both services' commands/
  views/JPA entities/web DTOs/mappers, `GlobalFieldTemplateService`'s
  `promote()` and `importOrReuse()`) changes to `systemId`.
- No FR currently covers a first-class system entity; see FR-56.
- Rule references, source-book metadata, or anything else system-level
  now has a natural home (`GameSystem`) to extend onto later — out of
  scope for this ADR, just the reason `GameSystem` is a real aggregate
  and not, say, a `CHECK`-constrained enum or a lookup table with no id.

## Alternatives considered
- **Keep `system` as free text, add an autocomplete sourced from
  distinct existing values.** Rejected — doesn't solve the typo/casing
  fragmentation problem (the actual motivation), and gives rule
  references nowhere stable to attach later.
- **Fold system management into `GlobalFieldTemplatesPanel` as a
  sub-form with no separate entity.** Rejected — same reasoning as
  above; the user explicitly wants a top-level, independently-existing
  concept, not a template-editor convenience.
