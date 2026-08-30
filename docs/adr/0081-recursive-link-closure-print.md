# ADR-0081: Recursive link-closure print scope

- Status: Accepted
- Date: 2026-08-30

## Context

The compendium print (`PrintView.tsx`, ADR-0035) always starts from
"everything in the chosen scope" (whole world or one campaign) and lets the
GM subtract articles via the per-article exclude checklist added this
session. For a dungeon crawl or similar tightly-linked set of articles, the
GM wants the opposite starting point: pick one article and print it plus
everything it `[[links]]` to, plus everything *those* link to — fully
recursive, no depth limit — without hand-picking from a world-wide list.

## Decision

- **A new "Linked from one article" toggle in the existing compendium print**
  (not a separate standalone print flow) — it reuses the existing print
  document, toolbar, `PrintOptionsMenu`, and per-article exclude checklist
  wholesale. When active it overrides the normal Scope select (disabled
  while a seed is picked) and always fetches the whole world, since a
  linked target can live outside any single campaign's reference set.
- **Closure computed by scraping `data-article-id` off each article's
  already-rendered `bodyHtml`**, not by re-parsing raw `[[..]]` markdown and
  reimplementing name/slug resolution client-side. `WikiLinker` only emits
  that attribute for a resolved link (a broken link renders as a `<span>`),
  so unresolved links are automatically excluded from the closure with no
  extra logic, and the closure can never drift from what the backend
  actually considers a valid link.
- **Unlimited depth, no cap**: the closure's `visited` set makes traversal
  terminate on its own — even through a link cycle — bounded by the total
  articles in the world, so no artificial depth limit is needed.
- **The closure only seeds the exclude checklist's default, once per seed
  pick** (tracked via a ref comparing the current seed to the last one
  applied) — an unrelated toolbar change (Maps, Tables, print options)
  re-triggers the load without re-computing or wiping out exclusions the GM
  already customized after picking a seed. Clearing the seed resets the
  checklist back to "everything included."

## Consequences

- No backend or contract change — the raw `body`/rendered `bodyHtml` this
  needs is already returned by the existing article endpoints; this is
  pure frontend logic over data already being fetched for the compendium
  print.
- "Exclude specific articles when printing" is satisfied for both this
  scope and the existing whole-world/campaign scopes by the same checklist
  — no separate exclusion UI was built for seed mode.

## Alternatives considered

- **A standalone "print from here" action on a single article**, separate
  from the compendium print — rejected for this pass: it would duplicate
  the toolbar, print-options, and exclude-checklist machinery that already
  exists, for a feature that fits as one more scope choice. Worth
  reconsidering if the compendium print's scope model gets too crowded.
- **Client-side re-parsing of raw `[[..]]` markdown** to compute the
  closure — rejected in favor of scraping `bodyHtml`'s already-resolved
  `data-article-id` attributes, which reuses the backend's own link
  resolution instead of duplicating it.
