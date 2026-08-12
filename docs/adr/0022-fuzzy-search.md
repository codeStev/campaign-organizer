# ADR-0022: Fuzzy article search via trigrams

- Status: Accepted (supersedes the search query of ADR-0017)
- Date: 2026-08-12

## Context
ADR-0017 implemented article search with a Postgres `tsvector` and
`plainto_tsquery`. In use this felt broken: full-text search matches whole word
**stems**, so typing a partial word like "water" returned nothing for
"Waterdeep", and typos ("watredeep") never matched. Users expect
search-as-you-type with partial and typo tolerance.

## Decision
Switch article search to **trigram matching** using the `pg_trgm` extension:

- A query matches when the title or body **contains** the term
  (`ILIKE '%q%'`, partial-word friendly) **or** the title is trigram-**similar**
  to the term (`similarity(title, q) > 0.15`, typo tolerant).
- Results rank by: title substring match first, then trigram similarity, then
  recency.
- A GIN `gin_trgm_ops` index on `title` keeps title matching fast; body uses
  `ILIKE` (acceptable scan at single-user scale).

`pg_trgm` is a trusted extension (PG 13+), so the app's database-owner role can
`CREATE EXTENSION` it in a Flyway migration.

## Consequences
- "water" → Waterdeep, "watredeep" → Waterdeep, "Jiar" → Jiarglung all work.
- Matching is substring/similarity based, not linguistic — no stemming (so
  "running" won't match "ran"). For worldbuilding proper-noun search this is the
  better trade-off.
- The `search_vector` column and its index from ADR-0017 remain in place but are
  no longer queried; kept for possible future ranked/FTS features rather than
  dropped in a migration.
- Body `ILIKE` scans the text; if a world grows very large, add a trigram index
  on a stripped-body expression (future ADR).

## Alternatives considered
- **`tsquery` prefix matching (`water:*`)**: fixes partials but not typos, and
  needs careful sanitizing of user input into a tsquery.
- **A dedicated search engine**: far too heavy for a personal, single-user app.
