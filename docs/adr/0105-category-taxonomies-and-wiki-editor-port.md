# 0105. Category taxonomies for Atlas/Handouts/Tables & Decks/Sheets, and the full Wiki editor port into `/next`

## Status
Accepted

## Context

ADR-0104 rebuilt `/next`'s Wiki sidebar around `Category` (a self-referencing,
`worldId`-scoped taxonomy that already had full backend CRUD but had never
been wired into any UI) and built the tree/drag-and-drop machinery to
support it. Once that pattern existed and worked, the user asked for the
same compact-tree-with-categories treatment on the four other in-world
screens that still used a flat list or tab strip: **Atlas** (maps),
**Handouts**, **Tables & Decks** (roll tables + card decks), and **Sheets**
(character sheets, statblocks, documents, field templates).

Separately, the user pointed out that `/next`'s Wiki article read view still
linked out to the old UI ("Edit in current UI →") for anything beyond
reading — body editing, revisions, AI draft, tags, template/parent fields.
Since old UI is slated for retirement (`docs/ui-overhaul-plan.md` Phase 7)
and can't remain the permanent editor, this needed a full native port, not
a permanent link-out: *"since we want to delete the old ui later you need
to port the functionality properly into the new one... we cant just link to
the old UI since that throws off the look and feel completely."*

Both pieces of work are covered by one ADR because they share the same
originating pattern (ADR-0104) and shipped in the same pass — extracting a
generic `<CategoryTree>` component made the first four slices possible, and
the article-editor port was the fifth destination for that same tree's
`onOpenEntity`/`onMoveEntity` wiring.

## Decision

### One `<CategoryTree>` component, five taxonomies

`NextWikiPage.tsx`'s tree/DnD logic (category rows, "Uncategorised" bucket,
`@dnd-kit/core` drag-and-drop, search-filters-in-place, auto-expand-to-active-
entity) was extracted into `frontend/src/components/CategoryTree.tsx`,
parameterized by category CRUD callbacks, an entity list plus
id/label/categoryId accessors, and an optional `renderEntityRow` override
(used where a row needs more than a label — Handouts' reorder/reveal
controls, Sheets' per-kind icon). Every one of the five screens below
renders the exact same component with the exact same CSS classes
(`.category-tree*`) — no per-screen visual variants.

`docs/architecture/architecture-harness.md`'s "no CRUD exemption" means
contexts never share tables or domain types, only
`application.port.published` interfaces. Following that, **each screen gets
its own taxonomy table**, mirroring `Category`'s own shape
(self-referencing `parent_id … ON DELETE CASCADE`; the owner's
`category_id … ON DELETE SET NULL`):

| Screen | Bounded context | Taxonomy table | Migration |
|---|---|---|---|
| Atlas (maps) | `worldbuilding` (sibling to `wiki`) | `map_categories` | V50 |
| Handouts | `handouts` | `handout_categories` | V51 |
| Tables & Decks | `tables` (spans `rolltable`+`carddeck`) | `table_deck_categories` | V52 |
| Sheets | `characters` (spans `sheet`/`statblock`/`document`/`template`) | `sheet_categories` | V53 |

Atlas *could* technically reuse Wiki's own `categories` table (same bounded
context) — the user explicitly chose a separate table anyway, a product
decision (maps and articles shouldn't share one taxonomy), not an
architecture constraint. Each owner's create/update use case validates the
incoming `categoryId` exists in-world before saving, mirroring
`ArticleService.validateCategory`.

### Per-screen shape, driven by product decisions the user made explicitly

- **Atlas** — its own taxonomy, separate from Wiki's (confirmed above). The
  map *picker* list in the sidebar becomes a `<CategoryTree>`; the existing
  pin-legend/pin-editor side column is untouched.
- **Handouts** — its own taxonomy. The tree replaces the flat list but keeps
  the existing `sortOrder` reorder buttons and revealed-toggle nested inside
  each row (`CategoryTreeEntityRow`'s wrapper changed from `<button>` to
  `<div role="button" tabIndex={0}>`, since a `<button>` can't legally
  contain another `<button>` — Wiki/Atlas don't nest controls, so this was a
  safe widening, not a behavior change for them).
- **Tables & Decks** — roll tables and card decks share **one** category
  tree, since they already share one screen/sidebar. A `TreeItem` union type
  (`{kind:'table', entity} | {kind:'deck', entity}`) merges both lists for
  the tree; each row keeps a 🎲/🃏 kind icon.
- **Sheets** — **one merged tree** spanning all four existing tabs
  (Characters, Statblocks, Documents, Templates); the tab strip is removed
  entirely. A `TreeItem` union across all four kinds, each with its own icon.
  `StatblocksPanel` keeps a *separate*, disconnected bulk-select section in
  the main pane for its print/encounter-builder checkbox list — asked via
  `AskUserQuestion` since the merged tree's "pick one to open" role and the
  bulk multi-select role are genuinely different interactions that don't
  collapse into one control. `GlobalStatblock`/`GlobalFieldTemplate` (the
  world-independent catalogs, ADR-0093) are explicitly out of scope: they
  aren't `worldId`-scoped, so they can't hang off a world-scoped category
  table.

Recurring implementation note across all four slices: a category-move
mutation must always send the owner's *full* existing state, not just
`categoryId` — every owner API's `update()` clobbers any field omitted from
the request body (no server-side "preserve if absent" semantics). This bug
class was caught and fixed in each slice's move-handler and, incidentally,
in a couple of existing save paths that predated this ADR
(`TemplateBuilder.tsx`'s save).

### Wiki editor: full port, not a link-out

Confirmed via direct read of the old UI's `WorldView.tsx` that every API the
article editor needs already exists (`articlesApi`, `articleTagsApi`,
`articleRevisionsApi`, `aiApi`, `templatesApi`) — this is a **frontend-only**
port, no migration or backend change. Also confirmed: **GM-only content was
never built** (ADR-0005/FR-15 explicitly dropped it — single-user app,
nothing to hide from), so the stale "GM-only block" mentioned in an earlier
`/next` doc comment was corrected, not implemented; `slug` (server-derived)
and `categoryId` (owned by drag-and-drop on the tree, not a form field) stay
out of the ported form too, matching the old editor's own scope exactly.

New `frontend/src/components/ArticleEditor.tsx` lifts the old editor's
fields and logic out of `WorldView.tsx`'s monolithic inline JSX into a
standalone component: title, template picker (with outline-seeding on first
pick), parent-article picker (candidates exclude self + all descendants),
tag input, the Markdown body editor with AI-draft wiring, save/cancel/delete,
a live preview, the "Used by" usages panel, and revision history with
tick-two-to-diff (`RevisionDiff`, reused verbatim — already
framework-agnostic). `NextWikiPage.tsx` now renders `<ArticleEditor>` with a
real read/edit mode toggle instead of the "Edit in current UI →` `NavLink`,
and gained a "+ New article" trigger (previously creation was only possible
through old UI, since `/next`'s Wiki had never had any write affordance
beyond the drag-and-drop category assignment ADR-0104 added). Category
assignment itself stays exactly as it was — drag-and-drop on the sidebar
tree — since the old editor never had a category field either; the one
change is that the article-move handler now always re-fetches the full
article rather than reusing a locally-cached copy, since `ArticleEditor` now
owns that state instead of `NextWikiPage`.

## Consequences

- Every world-scoped, listable entity type in this app (articles, maps,
  handouts, roll tables, card decks, character sheets, statblocks,
  documents, field templates) now has a category taxonomy and a `<CategoryTree>`
  sidebar — a consistent organizing pattern across the whole `/next` UI,
  landed incrementally by domain instead of all at once.
- Five separate taxonomy tables (six counting Wiki's own `categories`) is
  more tables than a single shared taxonomy would need, but keeps each
  bounded context owning its own data per the architecture harness, and
  matches the user's explicit preference that Atlas not share Wiki's.
- `/next`'s Wiki is now a fully self-sufficient article editor — the last
  piece keeping Phase 7 (old-UI retirement) from starting on Wiki
  specifically. `docs/ui-overhaul-plan.md` and `docs/requirements.md` are
  updated to reflect both halves of this ADR.
- Old UI (`WorldView.tsx`) is untouched by the editor port — it keeps its
  own, now-duplicate copy of the same editor logic until Phase 7 deletes it
  wholesale; not refactored to share `<ArticleEditor>` now, since old UI is
  going away rather than being maintained forward.

## Alternatives considered

- **One shared `categories` table across all five screens** — rejected: the
  architecture harness's "no CRUD exemption" means contexts don't share
  tables, and the user separately wanted Atlas's taxonomy kept distinct from
  Wiki's on product grounds, not just architecture ones.
- **Sheets keeps its tab strip, with a category tree per tab** — rejected by
  the user in favor of one merged tree (no tabs at all), matching the
  session's stated preference for a single organizing structure over
  parallel per-kind pickers.
- **Refactor old UI's `WorldView.tsx` to share `<ArticleEditor>`** instead of
  leaving it with its own duplicate copy — deferred: old UI is scheduled for
  wholesale deletion in Phase 7, so a shared-component refactor now would be
  thrown away shortly after, for no benefit in the interim.
