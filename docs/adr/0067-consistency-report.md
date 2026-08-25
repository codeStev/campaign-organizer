# 67. Per-world consistency report (FR-43)

Date: 2026-08-26
Status: Accepted

## Context

FR-25 built the usage index ("where is this article used?") and FR-40 added
wiki-link-bearing bodies outside articles (beats, roll-table entries, deck
cards). The inverse question — "what in this world is *wrong or stranded*?" —
had no answer: a typo'd `[[wiki-link]]` silently renders as plain text, an
article nobody links to quietly disappears from play, and content never
reached by any campaign is invisible dead weight. All of this is derivable
from data the backend already loads; nothing needs persisting.

## Decision

A read-only **consistency report** per world, computed on demand:

`GET /api/worlds/{worldId}/consistency-report` →

```json
{
  "brokenLinks":       [{ "sourceType": "ARTICLE|BEAT|ROLL_TABLE|CARD_DECK",
                          "sourceId": "...", "sourceLabel": "...",
                          "target": "lowercased link target" }],
  "orphanedArticles":          [{ "articleId": "...", "title": "..." }],
  "unreferencedByCampaigns":   [{ "articleId": "...", "title": "..." }]
}
```

Implementation lives in the existing `interchange/usage` context
(`ConsistencyReportService`), because it is the same reference machinery:

- **Link sources** are every body that goes through the wiki pipeline:
  article bodies, beat bodies (+ their *explicit* article-reference lists),
  each roll-table entry, each deck card. One shared
  `ArticleRenderPort.linkTargets` / `ArticleQueryPort.resolveRefs` pass —
  whatever `resolveRefs` cannot map to an article is broken.
- **Broken links** report one row per unresolved (source, target), targets
  de-duplicated and sorted within a source for stable output.
- **Orphans** = articles with no inbound resolved wiki-link and no explicit
  beat reference. Self-links do not rescue their own article.
  Pins, timeline events and relationships deliberately do *not* count as
  inbound references: they are structural metadata, not prose someone would
  look up.
- **Not used by any campaign** delegates to the existing
  `UsageQueryPort.articleIdsUsedInCampaign` aggregate over all campaigns of
  the world — prose links alone do not put an article "in play".

Frontend: a **Consistency** tab on the world view (`/worlds/{id}/consistency`,
deep-linkable like all tabs) listing the three sections with clickable links
into the offending article; a standalone print window reuses the ADR-0038
`NewWindowPortal` pattern for a paper checklist.

## Consequences

- No schema change, no new persistence; the endpoint is a pure read over
  published ports, so it stays correct as other contexts evolve.
- The report is a lint, not a gate: nothing blocks saving broken links.
- Cross-context access stays inside `interchange/usage`, which already holds
  the composition right for these ports; ArchUnit unchanged.
- Labels ("Beat: X — Arc", "Roll table: Y (3-4)") make printouts usable away
  from the screen; only ARTICLE sources are navigable in the UI since beats,
  tables and decks have no single canonical editor URL from here.
