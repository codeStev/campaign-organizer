# ADR-0017: Full-text search via a generated tsvector

- Status: Accepted
- Date: 2026-08-11

## Context
Article search (FR-7) initially used `ILIKE '%q%'`, which is substring-based,
unranked, and unindexed. We want word/stem-aware, ranked search that stays
inside the existing Postgres dependency (ADR-0003).

## Decision
Add a **`STORED` generated `tsvector` column** to `articles`, GIN-indexed:

- `title` is weighted `A`, `body` weighted `B`.
- HTML tags are stripped from the body (`regexp_replace(..., '<[^>]+>', ' ')`)
  before indexing so markup does not pollute the index.
- The English configuration is used explicitly (keeps the expression
  `IMMUTABLE`, which a generated column requires).

Search runs a native query:
`search_vector @@ plainto_tsquery('english', :q)`, ordered by
`ts_rank(...)` so title matches outrank body matches.

## Consequences
- Fast, indexed, stemmed, ranked search with no extra infrastructure.
- Matching is word/stem-based, not substring — searching `whisper` matches
  `whispering`, but `whisp` matches nothing. Acceptable for prose search.
- The generated column is maintained by Postgres automatically on
  insert/update; the entity does not map it.
- If multi-language or fuzzy/typo search is ever needed, revisit (trigram index
  or a dedicated engine) in a new ADR.

## Alternatives considered
- **Keep `ILIKE`**: simple but unranked, unindexed, and slow at scale.
- **Dedicated search engine (OpenSearch)**: overkill for a personal app; adds a
  service to run and sync.
