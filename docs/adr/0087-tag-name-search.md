# ADR-0087: Article search matches tag names

- Status: Accepted
- Date: 2026-09-01

## Context
Folksonomy tags (ADR-0083) let the GM mark an article "patron" without
touching its title or body. But the article search box (FR-7, ADR-0022 —
Postgres `pg_trgm` similarity plus `ILIKE` substring on title/body) has no
way to find that article by searching "patron": the word simply isn't in
the indexed text. The GM's mental model is "search for a thing," and a tag
is as much "the thing" as a title word.

The existing exact-match `tag` query param (a dropdown filter, ADR-0083)
already lets a GM filter by one specific tag, but it's a separate mechanism
from free-text `q` — today, if both are supplied in one request,
`ArticleService.list`'s precedence chain (`restrictToIds > query >
categoryId > all`) drops `q` entirely. That interaction is pre-existing and
out of scope here; this ADR is narrower: make the free-text search box
itself also check tag names, no new UI, no new request parameter.

## Decision
- **Stays behind `tagging`'s published port — no cross-context SQL join.**
  The existing title/body search is a native query against the `articles`
  table alone. Reaching into `tagging`'s `entity_tags` table from a native
  query in `worldbuilding`'s persistence adapter would hardcode another
  context's schema into this one, which is exactly what the
  `contextsOnlyUsePublishedPorts` ArchUnit rule exists to prevent — true
  even though both tables physically live in the same Postgres database.
  Tag matching is reached the same way the existing exact-match `tag`
  filter already is: through `ArticleTagLookupPort`
  (`worldbuilding`'s own out-port) → `WikiArticleTagLookupAdapter` (the
  anti-corruption adapter) → `tagging`'s published `TagQueryPort`.
- **New substring query, mirroring the existing exact-match one.**
  `TagQueryPort.entityIdsWhereTagContains(worldId, type, fragment)` is a
  case-insensitive substring match, added next to the existing exact-match
  `entityIdsTaggedWith` — same table (`entity_tags`), same normalization
  rule (names are already stored trimmed-lowercase, so the fragment is
  lower-cased by the caller and no `LOWER()` is needed on the column side).
- **Composition lives in `ArticleService`, not `ArticleController`.** The
  existing exact-match `tag` filter is wired in the controller (a
  pre-existing shortcut, untouched here), but this is relevance-ranking
  composition — building the actual result list for a search — which
  belongs in the application service next to `search()`. `ArticleService`
  gains `ArticleTagLookupPort` as a dependency.
- **Tag-only matches are appended after title/body matches, not
  interleaved.** Title/body results keep their existing order (title
  substring first, then trigram similarity, then recency). Articles that
  match only by tag are appended afterward, sorted by `updatedAt`
  descending. No attempt is made to compute a unified relevance score
  across two different matching mechanisms (trigram similarity vs. exact
  tag substring) — a simple, explicit two-tier order is easier to reason
  about and predict than a blended ranking would be.
- **No new repository batch-fetch method.** The tag-matched supplement
  reuses the exact in-memory-filter pattern `ArticleService.list` already
  uses for `restrictToIds` (`articles.findByWorld(worldId)` filtered by an
  id set) rather than adding a `findByIds` method that doesn't exist yet.
- **No new REST parameter, no response schema change** — only the `q`
  parameter's description in `docs/api/openapi.yaml` is reworded.

## Consequences
- A GM can find an article by any tag on it, not just title/body text,
  through the same search box they already use — no new UI to learn.
- The exact-match `tag` dropdown filter and its `q`-dropping interaction
  are unchanged; fixing that composition (if ever wanted) is a separate
  decision.
- Statblocks have no free-text search box today, so this doesn't extend
  there; a future statblock search would need its own decision.

## Alternatives considered
- **Extend the native SQL query to join `entity_tags` directly**: would
  perform slightly better in one round-trip, but bakes a cross-context
  table dependency into `worldbuilding`'s persistence adapter — rejected
  for the same reason every other cross-context read in this codebase goes
  through a published port instead of a raw join.
- **Blend trigram similarity and tag-match scores into one ranked list**:
  more "correct" relevance ordering, but requires inventing a scoring
  formula across two unrelated matching mechanisms for a personal,
  single-user tool where "tag matches show up right after text matches" is
  simple and sufficiently useful — rejected as premature complexity.
- **Fold this into the existing `tag` exact-match filter instead of `q`**:
  rejected — the user's own framing was "search for it," i.e. the
  free-text box, not the separate exact-match dropdown; the dropdown
  already does exact tag lookup correctly and isn't broken.
