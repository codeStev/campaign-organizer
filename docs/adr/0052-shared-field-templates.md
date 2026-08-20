# ADR-0052: Shared field templates for sheets and statblocks

- Status: Accepted
- Date: 2026-08-20

## Context
Statblock `stats` is a freeform `Map<String,Object>`: `StatblocksPanel` renders a
growable list of key/value text rows, so every new monster means retyping "AC",
"HP", "Speed", … from scratch (FR-18). Character sheets already solved this with
a schema-driven template engine — `SheetTemplate`, `FieldType`-typed fields
grouped into sections, a drag-and-drop builder, and a renderer that builds the
form purely from the definition (ADR-0024, ADR-0029, ADR-0030).

The question was whether statblocks get their own parallel `StatblockTemplate`
aggregate or reuse the sheet engine.

## Decision
**One template aggregate serves both kinds.** `SheetTemplate` is renamed to
`FieldTemplate` (it is no longer sheet-specific) and gains a `kind` discriminator
(`CHARACTER | STATBLOCK`), chosen once at creation. One editor
(`TemplateBuilder`), one renderer (`TemplateForm`, renamed from `SheetForm`), one
management panel (`FieldTemplatesPanel`) — the user just picks which kind they're
building.

- **Full palette for both kinds.** No field-type restriction for statblocks —
  circle trackers, SELECT, and BOOLEAN genuinely suit monsters (legendary
  resistances, size, flags), and restricting the palette would need a second
  code path in `TemplateBuilder` for no real benefit.
- **Sections**, exactly as character sheets have them.
- **Ship a builtin D&D 5e Monster starter**, mirroring `BuiltinFieldTemplates`'
  existing character-sheet starters.
- **`statblocks.template_id` is nullable.** Existing freeform statblocks keep
  working untouched; assigning a template later is opt-in, and unmatched
  `stats` keys stay visible and editable in an "Other stats" section rather
  than silently disappearing.
- **Delete semantics differ by kind.** Deleting a `CHARACTER` template still
  cascades to its sheets (unchanged ADR-0024 behaviour — a sheet without its
  template can't render). Deleting a `STATBLOCK` template `SET NULL`s
  `statblocks.template_id` instead: a monster is a going concern independent of
  its stat layout, so losing the template must fall the statblock back to
  freeform stats, not delete it.

## Consequences
- One engine, one builder, one panel to maintain instead of two near-identical
  aggregates.
- `FieldTemplateQueryPort` gains `findByWorldAndKind`; `StatblockService`
  validates a referenced template both exists in the world and has
  `kind = STATBLOCK` (an accidental character-sheet reference is a
  `ValidationException`, same shape as its existing article/campaign checks).
- Renaming touches ~30 backend files and the OpenAPI contract (paths, schemas,
  the `interchange` export bundle key `sheetTemplates` → `fieldTemplates`) —
  landed as its own mechanical commit, no behaviour change, before the `kind`
  work.
- `V22__field_templates.sql` renames the table in place (`ALTER TABLE
  sheet_templates RENAME TO field_templates`), so existing character-sheet
  templates and their data are untouched; `kind` defaults to `CHARACTER` for
  all pre-existing rows.

## Alternatives considered
- **A parallel `StatblockTemplate` aggregate** (domain/application/adapter,
  its own builder and panel): true separation, but ~18 duplicated files for a
  form that is otherwise identical, and a worse UX — the user would manage
  templates in two disconnected places for something they think of as one
  concept ("templates for stuff I fill out").
- **Restrict the statblock palette to TEXT/NUMBER** (the original ask): simpler
  builder, but arbitrarily blocks CIRCLES/SELECT/BOOLEAN for cases where they're
  a natural fit (spell slots, creature size, legendary-flag toggles), and still
  needs a second `TemplateBuilder` code path to enforce the restriction.
