# 0104. Wiki sidebar groups by Category, not parentArticleId, in /next

## Status
Accepted

## Context

ADR-0080 gave articles a `parentArticleId` for structural nesting and made
that field drive the sidebar tree, explicitly leaving `Category` (article
taxonomy, `parentId`-nested) as orthogonal and noting it "has no frontend UI
today besides" — i.e. deliberately unused in any screen at the time.

The mockup that's driving the `/next` UI overhaul (`docs/ui-overhaul-plan.md`)
shows the Wiki sidebar as a "BY CATEGORY" tree instead: nested categories
(e.g. Characters > Reckoners > Guild factors) each with an article count,
plus an "Uncategorised" bucket. The user confirmed (asked directly, given
this reverses part of ADR-0080's frontend choice) that the rebuild should
match the mockup's Category tree rather than just restyling the existing
parentArticleId tree.

Checking the backend first: `Category` already had full CRUD
(`CategoryController`: list/create/update/delete, `/worlds/{worldId}/categories`)
and an OpenAPI contract (`CategoryRequest`/`Category` schemas) — this was
already-built, already-documented backend capability, just never consumed by
any frontend page (old or new). `ArticleRequest.categoryId` was likewise
already wired through article create/update. So this is a **frontend-only**
change: no new migration, no new backend endpoint, Ground Rule 4's
"new backend capability needs an ADR + FR + migration first" doesn't apply
because nothing new is being added to the backend.

## Decision

- **`/next`'s `NextWikiPage.tsx` sidebar now groups articles by `Category`**,
  reproducing the mockup: a recursive tree built from `Category.parentId`,
  each node showing its name, a count of articles directly in it, and
  (on hover) "+" to add a sub-category and "✕" to delete it
  (`ConfirmDeleteDialog`, matching the API's own behavior: deleting a
  category just uncategorizes its articles, never cascades). An
  "Uncategorised" pseudo-node at the end covers articles with no category.
  Search filters the tree in place (title match, ancestor categories
  force-expanded) rather than falling back to a flat list.
- **`categoriesApi` in `client.ts` extended** with `create`/`update`/`remove`
  (previously `list()` only) — thin wrappers matching the existing OpenAPI
  contract, no backend change.
- **A category picker on the article read pane** (`Select`, defaulting to
  "— uncategorised —", options labeled with the full path e.g.
  "Places / Cities") — the first UI anywhere that lets a user set an
  article's category. This is deliberately narrow: it's the one write
  affordance added to an otherwise still-read-only `/next` Wiki (body
  editing stays on the old UI's richer editor per ADR-0080/Phase 2 scoping),
  justified because there was previously *no* way at all, in either UI, to
  assign a category.
- **`parentArticleId`-based nesting is not shown in `/next` at all now** —
  neither as the sidebar's structure (replaced by Category) nor as a
  breadcrumb on the read pane (not carried over from the old design). The
  field, its backend validation, and old UI's own parentArticleId tree are
  completely untouched; this is purely about what `/next`'s Wiki sidebar
  renders.
- **Old UI is untouched** (Ground Rule 2) — `WorldView.tsx`'s sidebar still
  nests by `parentArticleId`, still has no category picker. This means a
  category can currently only be assigned to an article via `/next`'s Wiki,
  while old UI's article editor remains the only place to edit an article's
  parent/body — a known, temporary cross-UI split until Wiki gets its own
  full old-UI-retirement pass (Phase 7).

## Consequences

- `Category` and `parentArticleId` remain orthogonal on the data model
  (ADR-0080's point stands) — this ADR only changes which one drives the
  **presentation** of `/next`'s Wiki sidebar, not the data model itself.
- A world with articles that only use `parentArticleId` nesting (no
  categories assigned) will show everything under "Uncategorised" in
  `/next`'s Wiki until categories are created and assigned — expected, not
  a bug, and matches what the mockup itself implies (an empty/flat
  Uncategorised bucket is a valid starting state).
- `docs/ui-overhaul-plan.md`'s Phase 5c bullet on Wiki is corrected to
  reflect this (category tree + picker), not just the tag-chips addition
  that shipped earlier in the same pass.

## Alternatives considered

- **Keep parentArticleId, just improve its visual nesting** — rejected by
  the user in favor of matching the mockup's actual mechanism, once it was
  established Category already has full backend support sitting unused.
- **Both trees at once** (Category as the primary grouping, with
  parentArticleId nesting *within* a category) — not attempted here; the
  mockup only shows one tree, and combining both would be a second,
  separate design decision better made deliberately later if a real need
  for both shows up, not folded silently into this pass.
