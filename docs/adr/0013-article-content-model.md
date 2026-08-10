# ADR-0013: Article content model (HTML body, slugs, templates)

- Status: Accepted
- Date: 2026-08-10

## Context
Phase 1 introduces the wiki: articles organized in categories. We must decide
how article bodies are stored, how articles are addressed, and how article
"types" (templates) are represented.

## Decision
- **Body format: HTML.** The TipTap editor (ADR-0002) emits HTML natively, so
  article bodies are stored as an HTML string in a `body TEXT` column. HTML
  renders directly, can be post-processed for auto-linking (FR-5), and can be
  stripped to plain text for full-text search (FR-7).
- **Slugs.** Each article has a URL-safe `slug`, **unique per world**,
  auto-generated from the title when omitted and de-duplicated with a numeric
  suffix (`goblin`, `goblin-2`). Slugs are the human-facing address and the
  target for auto-linking.
- **Templates as a type enum.** An article's template
  (`GENERIC, CHARACTER, LOCATION, ORGANIZATION, SPECIES, ITEM, EVENT`) is stored
  as a string. In Phase 1 it only classifies the article; structured
  prompts/fields per template come in FR-4.
- **Categories** form a hierarchy via a nullable self-referential `parent_id`.
  Deleting a category sets its articles' `category_id` to NULL (articles
  survive); deleting a world cascades to both.

## Consequences
- Simple, portable storage; rendering and search both work off the HTML.
- HTML from the editor must be **sanitized** on input to prevent stored XSS —
  tracked as a follow-up before the body is ever rendered unescaped.
- Slug de-duplication keeps URLs stable and unique without user friction.
- Storing template as a string (not a DB enum) keeps migrations simple when new
  types are added.

## Alternatives considered
- **ProseMirror JSON** instead of HTML: more faithful to the editor, but harder
  to search and to auto-link without rendering first.
- **Markdown**: portable and diff-friendly, but adds a render step and diverges
  from TipTap's native HTML output.
