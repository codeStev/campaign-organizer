# 0096. Global, system-scoped statblock catalog with copy-on-import

## Status
Accepted

## Context
ADR-0093 made character-sheet/statblock **templates** (structure —
sections/fields) global and system-scoped, referenced live from any world
via a dual-FK (`worldTemplateId`/`globalTemplateId`): editing a global
template's layout propagates everywhere it's used, by design. It
deliberately left statblock **instances** — actual monster/NPC data, real
HP/AC/stat values — untouched; those still only exist per world.

The user wants that solved too: a game-system-scoped catalog of real
monsters (e.g. "Adult Red Dragon" with actual numbers, keyed to D&D 5e)
reusable across every campaign that runs that system, in any world.
Critically, the user was explicit about the mechanism: *"on an import
basis... I have to explicitly import them to a campaign"* — a deliberate
one-time copy, not a live shared reference. This is the opposite of
ADR-0093's model on purpose: a system's character-sheet layout genuinely
is the same everywhere and benefits from staying live, but one campaign's
GM reskinning "Adult Red Dragon" for their world must never silently
mutate another campaign's copy (or the shared original).

## Decision

### A new aggregate, `GlobalStatblock`
World-independent, system-scoped, living in `characters/domain/statblock/`
(sibling to `Statblock`, not to `GlobalFieldTemplate` — it's statblock
data, not template schema):
```java
GlobalStatblock(id, systemId, globalTemplateId, name, stats, notes, createdAt, updatedAt)
```
`systemId` is required (`ON DELETE RESTRICT`, matching `GlobalFieldTemplate.systemId`).
`globalTemplateId` is optional, a **single** nullable FK to a
`GlobalFieldTemplate` — no dual-FK/world-tier split like `Statblock` has,
since this aggregate has no world tier at all. If set, the service
validates the referenced template has `kind == STATBLOCK` **and** the same
`systemId` as the catalog entry — a `GlobalStatblock` and the template
driving its guided form are both already system-scoped, so a cross-system
pairing is a real modeling error, not just an inconsistency to tolerate
(stricter than world `Statblock`, which may legitimately mix systems since
a world can run several).

Own full port set (`port.in`/`port.out`/`port.published`), own
`GlobalStatblockService`, own `GlobalStatblockController` at
`/api/statblocks/global` — same shape ADR-0093 established for
`GlobalFieldTemplate`.

### Import is copy-then-create, not a live reference
`POST /api/statblocks/global/{id}/import` (body: `worldId`, required
`campaignId`, optional `name` override) creates a brand-new world
`Statblock` by delegating to the existing `CreateStatblockUseCase` — the
same one `Statblock.duplicate()` already reuses (ADR-0089). The result
carries a plain copied `stats` map and (if the source had one) the same
`globalTemplateId`, but **no back-reference** to the `GlobalStatblock` it
came from. From that point on the two are fully independent: editing
either has zero effect on the other.

**`campaignId` is required on import**, unlike `Statblock.campaignId`'s
normal optional/"null = shared" semantics — per the user's explicit ask,
an imported monster should never land in world-shared limbo; it's always
imported *for* something.

### Delete semantics
`GlobalStatblock` deletion needs **no reference guard** — nothing FKs to
it, since import never leaves a live pointer back. This is a direct,
pleasant consequence of the copy-on-import design: unlike
`GlobalFieldTemplate.delete()` (blocked while any sheet/statblock still
references it), a `GlobalStatblock` can always be deleted freely.

`GlobalFieldTemplateService.delete()` does gain one addition, though: it
already blocks deletion of a template still referenced by a
`CharacterSheet`/`Statblock`; this ADR adds a third check against
`GlobalStatblockRepositoryPort`, since a `GlobalStatblock` is now also a
possible referencer of `global_field_templates` — without it, deleting an
in-use template would surface as a raw DB FK violation instead of the
existing friendly `ConflictException`.

### Interchange: same resolve-or-reuse exception as ADR-0093
`GlobalStatblockImportPort.importOrReuse(view)` resolves an existing
catalog entry by exact `(systemId, name)` match and reuses its id if
found, only creating fresh when genuinely new — the same deliberate
exception to the standard "always mint a fresh id" import contract that
`GlobalFieldTemplate`/`GameSystem` already get, for the same reason
(don't fragment a shared catalog on repeated backup re-import).

## Consequences
- New table `global_statblocks` (migration `V45`), new REST route family,
  new frontend page (`GlobalStatblocksPanel.tsx`) alongside the existing
  global-templates page, both reachable from the same top-level
  "🧩 Templates" nav entry.
- `StatblocksPanel.tsx` gains an "Import from catalog" action alongside
  its existing "+ New statblock" and `duplicate()` actions.
- `ExportService`/`ImportService` gain one more leaf catalog entity,
  resolved the same way `globalFieldTemplates`/`gameSystems` already are.
- No changes required to `Statblock`/`StatblockService` beyond
  `GlobalStatblockService` depending on `CreateStatblockUseCase`.
- No FR currently covers this; see FR-58.

## Alternatives considered
- **Live reference (dual-FK, like `GlobalFieldTemplate`).** Rejected —
  directly contradicts the user's explicit "on an import basis" ask, and
  is the wrong semantics for *data* (meant to be tuned per use) versus
  *structure* (meant to stay consistent everywhere).
- **Reuse `Statblock` itself with a nullable `worldId`.** Rejected —
  `worldId` is required throughout `Statblock`'s existing query and
  validation paths (`findByIdAndWorld`, JPA FK `CASCADE`, `requireWorld`
  checks); overloading it with nullable semantics would ripple through
  every existing call site. A dedicated aggregate is a smaller, cleaner
  diff, matching ADR-0093's own precedent of adding a new aggregate for a
  world-independent catalog rather than retrofitting the existing one.
- **Fold `stats`/`notes` onto `GlobalFieldTemplate` itself.** Rejected —
  conflates one shared schema with many distinct instances; the entire
  point of `GlobalFieldTemplate` is that many statblocks can share one
  template.
