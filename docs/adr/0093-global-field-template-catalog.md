# 0093. Global, system-scoped field template catalog

## Status
Accepted

## Context
`FieldTemplate` (character sheet and statblock schemas, ADR-0024/ADR-0052)
is world-scoped: every world that runs the same game system (e.g. D&D 5e)
currently needs its own hand-rebuilt copy of that system's character
sheet and monster-statblock templates. A world can run multiple campaigns
in different systems, and the same system routinely recurs across
unrelated worlds — the template belongs to the *system*, not the world.

`FieldTemplate` already has a `system` string field, but today it's purely
a label; nothing treats it as a sharing key.

## Decision

### A new, genuinely world-independent aggregate: `GlobalFieldTemplate`
Lives alongside `FieldTemplate` in the same `characters` bounded context
and package family (`characters/domain/template/`, sharing `TemplateKind`/
`TemplateSection`/`TemplateField` from `FieldSchema` as-is). Shape mirrors
`FieldTemplate` minus `worldId`:
```java
GlobalFieldTemplate(id, name, kind, system, sections, createdAt, updatedAt)
```
`system` is **required and non-blank** here (unlike `FieldTemplate`, where
it stays an optional label) — for a global entry, the system *is* the
reason it exists. No uniqueness constraint on `(kind, system)`: nothing
stops two global variants of "D&D 5e" existing side by side if a GM
genuinely wants that; the point is to make sharing *possible*, not to
police it.

This is a new pattern for this codebase — no prior aggregate lacks a
`worldId` and is referenced by FK from world-scoped children (confirmed
by exploration; `ai_provider_settings` is the only world-independent
table, but it's a singleton with no inbound references, not a catalog).
Deliberately kept inside `characters` rather than promoted to its own
bounded context: unlike `tagging` (extracted because two *unrelated*
contexts needed it, ADR-0083), the global catalog is consumed only by
`characters`' own `CharacterSheet`/`Statblock` — same justification
already used in ADR-0092 to keep `Todo` inside `campaign`.

**Scope: `CHARACTER` and `STATBLOCK` kinds only.** `DOCUMENT`-kind
templates (ADR-0088) stay purely world-scoped — a session-zero handout or
tone document isn't "a game system" the way a class sheet or monster
block is, and the issue driving this ADR only named character sheets and
statblocks. `Document`'s `template_id` is untouched by this ADR; the same
dual-reference pattern below could be extended to it later if a concrete
need appears, following this ADR as the template.

### Dual nullable-FK reference, not a discriminator column
`CharacterSheet.templateId` and `Statblock.templateId` each become **two**
nullable FK columns — `worldTemplateId` (→ `field_templates`, unchanged
semantics) and `globalTemplateId` (→ `global_field_templates`, new) — with
a `CHECK` constraint on each table enforcing the right cardinality:
- `character_sheets`: `CHECK (num_nonnulls(world_template_id, global_template_id) = 1)`
  — a sheet always needs exactly one template (unchanged requirement,
  just satisfiable from either source).
- `statblocks`: `CHECK (num_nonnulls(world_template_id, global_template_id) <= 1)`
  — the existing freeform-fallback allowance (ADR-0052: statblock template
  is optional) is preserved; both may be null.

This is a genuine gap in the codebase (no dual-parent-FK precedent
existed before this ADR) but was chosen over a single `templateId` column
plus a `templateScope` discriminator specifically to preserve this
codebase's existing belt-and-suspenders style: every template reference
today has *both* a real DB FK *and* an app-level kind check (see
`CharacterSheetService`/`StatblockService`'s `validateLinks`). A
discriminator-only design would silently drop DB-level referential
integrity for whichever source isn't chosen — worse than what exists
today, not neutral.

**`global_template_id`'s `ON DELETE` policy is `RESTRICT`** on both
tables, deliberately different from `world_template_id`'s existing
policy (`CASCADE` on `character_sheets`, `SET NULL` on `statblocks`). A
global template's blast radius spans every world that uses it; silently
cascading a delete across all of them is a much bigger and more
surprising loss than the existing per-world cascade. Deleting an in-use
global template must be a blocked, explicit, "detach these sheets first"
operation.

### One-time consolidation: a Flyway Java migration, not an app-layer runner
No migration in this repo has ever done a data transformation (grouping,
canonical-selection, repointing FKs, deleting rows) — every one of the 38
existing migrations is pure schema DDL. This genuinely is a one-time data
transformation, and Flyway's own versioned, run-exactly-once guarantee
(tracked in `flyway_schema_history`, the same bookkeeping every other
migration already relies on) is the correct tool for that — closer to
house style than either raw dedup SQL (no precedent, awkward to express
"pick most-recently-updated" in SQL) or a Spring `ApplicationRunner`
(would either need its own "have I already run" bookkeeping, reinventing
what Flyway already tracks, or run unboundedly on every startup — see
below for why that's undesirable here).

Split into two migrations:
- **`V39__global_field_templates.sql`** — schema only: create
  `global_field_templates`; rename `template_id` → `world_template_id` on
  `character_sheets`/`statblocks`; drop the `NOT NULL` on
  `character_sheets.world_template_id`; add both `global_template_id`
  columns and both `CHECK` constraints.
- **`V40__ConsolidateDuplicateFieldTemplates.java`** (Flyway
  `BaseJavaMigration`, plain JDBC — Flyway migrations run before the
  Spring context exists, so no repositories/services here): groups every
  `field_templates` row by `(kind, lower(trim(system)))`, skipping blank
  `system`. For any group whose rows span **more than one `world_id`**,
  picks the most-recently-updated row as the content seed, inserts one
  `global_field_templates` row from it, repoints every
  `character_sheets`/`statblocks` row referencing *any* template id in
  that group to the new global id (`world_template_id = NULL,
  global_template_id = <new id>`), then deletes every `field_templates`
  row in the group. Groups confined to a single world are left
  untouched — that's not duplication, it's legitimately one world's
  local template.

**Deliberately one-time, not an ongoing background dedup.** `system` is
free-text and prone to accidental collisions (two unrelated homebrew
templates both just called "homebrew"); auto-merging newly-created
same-named templates on every future startup would be a standing source
of surprise data loss. Running this once, as a normal versioned
migration, cleans up the *existing* backlog of duplication (the actual
problem today) without adding permanent magic behavior. Going forward,
consolidating a *new* template is the explicit "promote to global" action
below — a deliberate GM action, not an automatic one.

### "Promote to global" — the same repoint logic, exposed as one action
`POST /worlds/{worldId}/field-templates/{templateId}/promote` — a new
route on the existing world-scoped template. Creates one
`GlobalFieldTemplate` from the source's `name`/`kind`/`system`/`sections`,
repoints every `CharacterSheet`/`Statblock` in that world referencing the
source template to the new global one, then deletes the source
`FieldTemplate` — literally the migration's per-group logic run against a
group of one, shared as one service method between the startup migration
and this endpoint. Restricted to `CHARACTER`/`STATBLOCK` kinds (same
scope limit as the rest of this ADR). No merge/diff UI, matching
ADR-0089's "no smart defaults" precedent — it's a straightforward
promote, not a reconciliation tool.

### Import/export: resolve-or-create, not blind recreate
Every other published import port (ADR-0061) always mints a fresh id on
import, specifically so re-importing a backup never collides with data
that's still there. Applied naively to `GlobalFieldTemplate`, that would
recreate a duplicate global template on every re-import of a world backup
that references one — defeating the entire point of this ADR.
`GlobalFieldTemplateImportPort.importOrReuse(view)` instead resolves an
existing global template by exact `(kind, system, name)` match and reuses
its id if found; only creates a fresh row when truly new. This is a
deliberate, documented exception to the standard import-port contract.

## Consequences
- `character_sheets` and `statblocks` gain a second nullable FK column
  each, plus a `CHECK` constraint; every service/mapper/DTO touching
  `templateId` on these two aggregates changes to a
  `worldTemplateId`/`globalTemplateId` pair (exactly one set).
  `TemplateBuilder`/`TemplateForm` (frontend) are unaffected — they
  operate on `sections` content, not on where the template lives.
- A new world-independent route family:
  `GET/POST /field-templates/global`,
  `GET/PUT/DELETE /field-templates/global/{templateId}` (mirrors the
  existing world-independent `/field-templates/builtin` route's
  no-`{worldId}` shape).
- The character-sheet/statblock "template" picker in the frontend now
  merges two lists (this world's + the global catalog) instead of one,
  labeled by source.
- `BuiltinFieldTemplates` (the static in-code "starter" list) is
  untouched — "Add starter" into a world keeps its existing copy-into-
  this-world behavior unchanged (still a legitimate flow: start from
  D&D 5e, then heavily reskin just for this world). A separate "Add
  starter" entry point on the new global-catalog panel adds the same
  static list directly into the global catalog instead (idempotent per
  `(kind, system, name)`).
- No FR currently covers cross-world template reuse; see FR-55.

## Alternatives considered
- **Drop `worldId` from `FieldTemplate` entirely, single-tier.** Rejected
  by the user explicitly: existing one-off/homebrew per-world templates
  are a real, wanted use case, not something to force into the global
  catalog.
- **Single `templateId` + discriminator column, no second FK.** Rejected
  — see "Dual nullable-FK reference" above; would regress the existing
  belt-and-suspenders (FK + app check) style to app-check-only.
- **Continuous background auto-merge on every startup.** Rejected in
  favor of a one-time versioned migration — see "Deliberately one-time"
  above.
- **Extend `Document` (ADR-0088) too.** Deferred, not rejected — out of
  the scope the issue actually named; same pattern applies cleanly later.
