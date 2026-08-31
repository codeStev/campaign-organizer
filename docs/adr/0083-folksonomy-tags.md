# ADR-0083: Folksonomy tags for articles and statblocks

- Status: Accepted
- Date: 2026-08-31

## Context
Articles and statblocks pile up fastest of any content type, and the
existing category/parent-child hierarchy (ADR-0080) forces a single
structural home per article chosen up front. The owner wants an orthogonal,
ad-hoc way to mark things ("recurring villain", "session 1 prep", "unused")
without committing to that hierarchy — a folksonomy: freeform strings, no
`type:`/`status:` namespacing, no colors/icons, no forced taxonomy.

Issue #58 suggested modelling this "the same shape as how media attachments
already work across contexts." Investigating the actual `media` bounded
context surfaced a correction worth recording: **media is not keyed by a
polymorphic `(entityType, entityId)`.** `media` rows are plain world-scoped
assets (`id, worldId, filename, ...`); each *consumer* (e.g. `WorldMap`)
stores its own `mediaId` column and validates it through `MediaLookupPort`.
There is one media row per asset, referenced by exactly one owner at a time.

Tags need the opposite shape: **one tag can apply to many entities, and one
entity can carry many tags**, across two different bounded contexts
(`worldbuilding` articles, `characters` statblocks) with a shared,
world-scoped autocomplete vocabulary and a cross-entity browse view. A
genuine `(entityType, entityId) → tags[]` store is therefore new to this
codebase, not a copy of the media pattern — the closest existing precedent is
how `interchange` composes other contexts purely through their published
ports (`ConsistencyReportService` for FR-43, `ExportService`/`ImportService`
for FR-36).

## Decision
- **New bounded context `tagging`** — a generic supporting context, same
  rank as `media` — owns tag storage end-to-end. `worldbuilding` and
  `characters` never persist tag data themselves; they only consume
  `tagging`'s published port to filter their own lists.
- **One table, no canonical `Tag` identity.** `entity_tags(id, world_id,
  entity_type, entity_id, name, created_at)`, unique on `(world_id,
  entity_type, entity_id, name)`. The issue's own non-goals rule out
  rename/merge/global tag management, so a normalized `Tag` aggregate would
  buy nothing; the distinct-name list for autocomplete and the browse view is
  a plain `SELECT DISTINCT` over this one table.
- **Names are folded to trimmed lowercase** at the domain boundary, on both
  write and read. Without this, "Villain" and "villain" would silently
  fragment into two entries in autocomplete and the browse view — there is no
  management screen to merge them back, so the domain enforces one canonical
  form up front instead.
- **Whole-set replace, not add/remove.** `SetEntityTagsUseCase` takes the
  full desired tag set for one entity and replaces it in a single
  transaction, matching the read/edit split's "edit locally, commit on Save,
  discard on Cancel" convention already used for every other field (fixed for
  Cancel specifically in 280c0aa) rather than firing an API call per chip.
- **Existence validation via anti-corruption adapters**, same shape as
  `handouts`'s `HandoutWorldExistsAdapter`/`HandoutSessionExistsAdapter`:
  `TaggingWorldExistsAdapter` over `worldbuilding`'s world lookup,
  `TaggingArticleExistsAdapter` over `ArticleQueryPort`,
  `TaggingStatblockExistsAdapter` over `StatblockQueryPort`.
- **List filtering** (`GET .../articles?tag=`, `GET .../statblocks?tag=`)
  is implemented in `worldbuilding` and `characters` themselves, each behind
  a small out-port backed by an anti-corruption adapter over `tagging`'s
  published `TagQueryPort.entityIdsTaggedWith(worldId, entityType, name)` —
  the same pattern as the existing `campaignId` filter.
- **Cross-entity browse-by-tag lives in `interchange`**, not in `tagging`:
  `GET /worlds/{worldId}/tags/{tagName}/entities` composes `tagging`'s
  `TagQueryPort` with `worldbuilding`'s `ArticleQueryPort` and
  `characters`'s `StatblockQueryPort` to resolve titles/names — the same
  cross-context composition role `ConsistencyReportService` already plays.
- **Tags join the FR-36 backup bundle**: `tagging` publishes
  `TagImportPort`; `ExportService`/`ImportService` gain a `tags` key,
  remapped on import like every other entity-id reference.
- **v1 entity coverage is articles and statblocks only** (`EntityType` enum:
  `ARTICLE`, `STATBLOCK`); handouts, tables/decks, and character sheets are a
  later expansion, added by extending the enum and adding one more
  anti-corruption adapter — the store itself needs no change.

## Consequences
- Adding a third taggable entity type later is a small, additive change
  (enum value + one exists-adapter + one filter out-port); the `tagging`
  context and its persistence never change shape.
- Tag names have no identity of their own — renaming a tag means bulk-editing
  every row with that name (a script, not a UI action), which is acceptable
  because the issue explicitly excludes a management screen for v1. If that
  changes later, it is a new decision, not a bug here.
- Case-folding is a one-way, silent transform: a user who types "NPC" and
  later "npc" gets one tag, not two, which is very likely what a single user
  wants but is a deliberate loss of the original casing.
- `tagging` is a new dependency for `worldbuilding`, `characters`, and
  `interchange` (via published ports only), and for `interchange.export`'s
  bundle format (a new top-level `tags` array, additive and backward
  compatible with older exports that simply lack it).

## Alternatives considered
- **Store `mediaId`-style FK columns directly on `Article`/`Statblock`**
  (e.g. a `tags text[]` column per entity, mirroring media's per-consumer
  ownership): rejected because it would duplicate the tag-storage and
  autocomplete/browse logic once per entity type instead of once in a shared
  context, and would not generalize cleanly to the "later expansion" entity
  types the issue already anticipates.
- **A canonical `Tag` aggregate with a join table** (proper many-to-many,
  `tags` + `entity_tag_links`): more "correct" relationally, but buys nothing
  without rename/merge, and adds a second table plus a mapping layer for no
  behavioural gain in v1.
- **Browse-by-tag inside the `tagging` context itself**: would make `tagging`
  depend on `worldbuilding`'s and `characters`' published ports just to
  resolve display titles, inverting its role as a generic supporting
  context consumed by others. Keeping that composition in `interchange`
  (which already plays this role for FR-43) keeps `tagging` dependency-free
  of the contexts it serves.
