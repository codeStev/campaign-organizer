# ADR-0080: Article parent/child hierarchy

- Status: Accepted
- Date: 2026-08-30

## Context

A dungeon crawl or other complex location produces one flat `Article` per
sub-location plus one for the parent — a 10-room dungeon is 11 unsorted
articles in the world's flat sidebar list, which feels messy and doesn't
reflect the actual structure of the content.

The existing `Category` field on `Article` is a taxonomy (what kind of thing
is this), not a structure (what is this physically/narratively part of), and
has no frontend UI today besides. What's needed is real article-to-article
nesting: a sub-location article should visually nest under its parent
location in the sidebar (collapsed by default), while remaining a fully
independent article of any template type, still individually searchable,
wiki-linkable, printable, and pickable from beats/map pins exactly as today.

## Decision

- `Article` gains a nullable, self-referencing `parentArticleId`
  (`ALTER TABLE articles ADD COLUMN parent_article_id UUID REFERENCES
  articles(id) ON DELETE SET NULL`, migration `V31__article_parent.sql`),
  independent of `categoryId`. Unlimited nesting depth, and no restriction by
  `ArticleTemplate` in either direction — any article can parent any other.
- **Cycle prevention** in `ArticleService.validateParent`: rejects a missing/
  foreign parent, self-parenting, and multi-hop cycles by walking the
  proposed parent's ancestor chain looking for the article being updated.
  This is intentionally **stronger** than `Category`'s existing cycle check
  (which only rejects direct self-parenting, not multi-hop cycles) — a
  silent multi-hop cycle in the article tree would corrupt the sidebar with
  no easy fix, and the feature is explicitly meant to support deep,
  flexible nesting.
- **On parent deletion: `ON DELETE SET NULL`**, never cascade. Children
  survive as top-level articles. This mirrors the existing
  `articles.category_id → categories.id` FK (also `SET NULL`) and matches
  the hard requirement that children stay fully independent content a
  parent deletion must never destroy.
- **Consistency report**: a child with a parent is *not* flagged as
  "orphaned" in `ConsistencyReportService.findOrphans`, even with zero
  prose wiki-links or beat references. Being nested under a parent is
  itself a legitimate, primary way to reach an article (sidebar tree +
  the parent's Used-by panel); flagging every tidily-organized child as
  orphaned forever would just relocate the clutter problem into the
  consistency report and undermine trust in it.
- **Usage panel**: a new `CHILD_ARTICLE` usage type surfaces each child on
  its *parent's* "Used by" panel (`UsageService.articleUsages`), by direct
  analogy to how `MapPin.articleId` surfaces as a `MAP_PIN` usage on the
  article it points to — a child's `parentArticleId` is structurally the
  same kind of pointer. A child's own "part of X" indicator is a
  breadcrumb on the child's read view instead, not a usage entry on the
  child's own panel — "Used by" means "who points at me," and a parent
  doesn't "use" its child in that sense.
- **Frontend**: a "Parent article" picker in the edit form (excluding the
  article itself and its descendants), a collapsed-by-default sidebar tree
  grouped by `parentArticleId`, and a one-line "Part of `<parent>`"
  breadcrumb on a child's read view.
- **Import/export**: `parentArticleId` is remapped through the same id
  table as every other cross-reference on import (`ImportService`), the
  same way `sessionId` is remapped for handouts.

## Consequences

- `categoryId` (taxonomy) and `parentArticleId` (structure) are orthogonal
  and both remain on `Article` independently — a child can belong to a
  completely different category than its parent (e.g. a "Trap Room" child
  under a "Goblin Warren" parent, categorized under "Hazards" while the
  parent is under "Locations"). This is not a replacement for categories.
- `ArticleRevision` does **not** carry `parentArticleId` — revisions
  snapshot content only (title/slug/template/body), matching the existing
  precedent that `categoryId` isn't part of the revision snapshot either;
  restoring a revision preserves the article's *current* parentage.
- No new backend endpoint: `parentArticleId` rides the existing article
  create/update request and response, the same way `sessionId` does for
  handouts (ADR-0077).

## Alternatives considered

- **Reusing the `Category` hierarchy** instead of a new field — rejected,
  it conflates taxonomy with structure and the user explicitly wanted
  article-to-article nesting, independent of how an article is categorized.
- **One-level-only nesting** — rejected: the cycle check is depth-agnostic,
  so restricting to one level would need its own extra validation for no
  real benefit, and it would work against the explicit requirement for
  maximum flexibility (a vault inside a room inside a wing inside a
  dungeon should be representable).
