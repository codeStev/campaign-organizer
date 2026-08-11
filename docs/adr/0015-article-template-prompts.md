# ADR-0015: Article template prompts as backend metadata

- Status: Accepted
- Date: 2026-08-11

## Context
Each article template (CHARACTER, LOCATION, …) should guide the writer with
structured prompts — the sections World Anvil pre-fills (FR-4). We must decide
where those prompt definitions live.

## Decision
Serve template definitions from the backend as **static metadata** at
`GET /api/article-templates`: for each template, a display `label` and an
ordered list of `sections` (each a `heading` plus a short `hint`).

The frontend uses this to **seed a new article's body** with an outline
(`<h2>` headings) when a template is chosen and the body is still empty. The
definitions are code-defined (an enum-driven provider), not stored in the
database — they are application configuration, not user data.

## Consequences
- One source of truth for prompts, shared by any client and covered by a test.
- Adding/adjusting a template's prompts is a code change, not a migration.
- Seeding is non-destructive (only when the body is empty); the writer stays in
  control of the actual content.

## Alternatives considered
- **Hardcode prompts in the frontend**: quicker, but splits the definition from
  the `ArticleTemplate` enum and can't be reused or tested server-side.
- **Store templates in the database**: needed only if users could define their
  own templates, which is out of scope for now.
