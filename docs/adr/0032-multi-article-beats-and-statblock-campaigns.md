# ADR-0032: Multi-article beats and campaign-scoped statblocks

- Status: Accepted (extends ADR-0023, ADR-0031)
- Date: 2026-08-14

## Context
Two model refinements requested while running multiple campaigns in one world:
1. A story beat should link **several articles** (the place, a relevant NPC, an
   item), not just one — beats currently carry a single `article_id`.
2. **Statblocks** should optionally belong to a **campaign** (e.g. a specific
   boss), the same way character sheets already can (ADR-0031).

## Decision

### Multi-article beats
Replace the single `arc_beats.article_id` with a **join table
`beat_articles(beat_id, article_id)`**, both FKs `ON DELETE CASCADE` (deleting a
beat or an article removes the link). The existing single link is backfilled into
the join table, then the column is dropped. `ArcBeat` maps the links as a JPA
`@ElementCollection` of article ids (`List<UUID> articleIds`); the beat's
`sessionId` stays single. `BeatRequest`/`BeatResponse` change `articleId` →
`articleIds` (an ordered list). The arc board UI links articles via an
add-from-dropdown chip list.

### Campaign-scoped statblocks
Add an optional nullable `campaign_id` to `statblocks` (`ON DELETE SET NULL`),
mirroring character sheets exactly: validated to belong to the world, an optional
`?campaignId=` list filter, and a campaign selector/field in the statblocks UI.
Null = shared.

## Consequences
- Beats become richer scene records ("meet [NPC] at [place]") without extra
  articles; the join table also makes article→beat usage queries (ADR-0033)
  straightforward.
- A campaign can have its own bosses while the shared bestiary stays shared;
  deleting a campaign unlinks its statblocks (keeps them).
- `articleId` on beats is a **breaking API rename** to `articleIds`; acceptable —
  single-user, and the frontend moves in lockstep. Existing single links migrate.

## Alternatives considered
- **JSONB `article_ids` array on the beat**: simpler but loses referential
  integrity (a deleted article would leave a dangling id); the join table cleans
  up automatically.
- **Keeping `article_id` and adding a separate multi-link table**: two ways to
  express the same thing; rejected for one clear representation.
