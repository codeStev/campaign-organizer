# ADR-0021: Relationship graph model

- Status: Accepted
- Date: 2026-08-12

## Context
FR-11 adds relationship webs / family trees: connections between articles
(characters, organizations, locations) such as "parent of", "ally of", "member
of". The frontend renders these as a graph.

## Decision
- **Relationships are edges between two articles** in the same world:
  `from_article_id`, `to_article_id`, a free-text `label` (e.g. "parent of",
  "rival of"), and a `directed` flag.
- **No relationship-type table.** The label is free text; a curated vocabulary
  is unnecessary for a personal tool and would add friction. Common labels can be
  suggested client-side later.
- **Directed vs. undirected per edge.** "parent of" is directed (arrow);
  "allied with" is undirected. Rendering uses the flag; storage always records a
  `from`/`to` pair, and for undirected edges the order is not meaningful.
- **The graph is derived, not stored.** There is no graph entity — the
  relationships of a world *are* the graph. The API exposes:
  - `GET /worlds/{id}/relationships` — all edges in the world (for a full web).
  - `GET /worlds/{id}/articles/{articleId}/relationships` — edges touching one
    article (its immediate neighbourhood / ego graph).
- Endpoints validate that both endpoints are articles in the world; self-links
  (`from == to`) are rejected. Relationships are world-scoped and cascade-delete
  with either endpoint article (`ON DELETE CASCADE` on both FKs).

## Consequences
- Simple, flexible model that covers family trees, faction webs, and ad-hoc
  links with one table.
- The frontend owns layout (force-directed / tree); the backend stays a plain
  edge store.
- Deleting an article removes its edges automatically — no dangling links.
- Free-text labels mean no server-side grouping by type; acceptable, and
  groupable client-side if wanted.

## Alternatives considered
- **Typed relationships with an enum/table**: more structure than a single user
  needs; harder to extend on the fly.
- **Storing a graph/layout per world**: couples data to a particular
  visualization; layout belongs in the client.
