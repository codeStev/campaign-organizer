# ADR-0014: Wiki auto-linking via `[[target]]` syntax

- Status: Accepted
- Date: 2026-08-11

## Context
A wiki's value comes from dense cross-linking between articles (FR-5). We need a
way to author links to other articles that stays correct as titles change, and
that does not produce false positives.

## Decision
Use **explicit wiki-link syntax** authored in the body:

- `[[target]]` — link to the article whose **slug** or **title** matches
  `target` (case-insensitive).
- `[[target|label]]` — same, but render `label` as the link text.

Links are **resolved at read time**, not stored resolved. The raw body (with
`[[...]]`) is what's persisted and edited; each article response also carries a
derived, read-only **`bodyHtml`** with links rendered:

- resolved → `<a class="wiki-link" data-article-id="{id}" href="#">text</a>`
- unresolved → `<span class="broken-link">text</span>`

The frontend intercepts clicks on `.wiki-link` (via `data-article-id`) to
navigate in-app.

## Consequences
- Links follow renamed articles automatically (resolution is by current
  slug/title at render time), and never produce accidental matches.
- Editing shows the literal `[[...]]` tokens — simple and predictable; a live
  preview pane renders `bodyHtml`.
- Link text is HTML-escaped on render to avoid injection through labels.
- Rendering loads the world's article refs (id/slug/title projection) per read;
  fine at personal scale, cacheable later if needed.

## Alternatives considered
- **Automatic detection** (scan body for any article title): fragile, ambiguous,
  and costly; rejected.
- **Storing resolved HTML**: breaks when titles change and complicates editing.
