# ADR-0033: Article usage backlinks and campaign-usage filter

- Status: Accepted
- Date: 2026-08-14

## Context
With multiple campaigns sharing one world, the user wants to see **where an
article is used** — both a per-article "used by" panel and a way to filter the
article list to what a given campaign actually references. Articles are the hub:
they are referenced by beats, map pins, timeline events, relationships, character
sheets, statblocks, and other articles' `[[wiki-links]]`.

## Decision

### Read-only usage aggregation
A `UsageService` computes, for an article, a **flat list of usages**. Each usage
is `{ type, label, targetId?, campaignId?, campaignName? }` where `type ∈
{BEAT, MAP_PIN, TIMELINE_EVENT, RELATIONSHIP, CHARACTER_SHEET, STATBLOCK,
ARTICLE_LINK}`. Names are resolved server-side (arc/map/timeline/other-article
titles, campaign names) so the client renders and navigates without extra
lookups. `ARTICLE_LINK` backlinks are found by scanning other articles' bodies
for `[[target]]` tokens (reusing the `AutoLinker` link pattern) that resolve to
this article's slug or title. Exposed at
`GET /worlds/{worldId}/articles/{articleId}/usages`; rendered as a "Used by"
section in the article read view.

A flat, typed list (not a nested per-type object) keeps the contract small and
the UI generic, and makes adding new reference types additive.

### Campaign-usage filter on the article list
`GET /worlds/{worldId}/articles?campaignId=…` returns the articles **used in that
campaign** — i.e. referenced by any beat in the campaign, or by a character sheet
or statblock assigned to it. The Articles sidebar gets a campaign dropdown. The
filter is standalone (it ignores `q`/`categoryId`; the client can still narrow
further).

## Consequences
- One place to see everything that touches an article, with campaign context;
  and a campaign lens over the shared wiki.
- Aggregation issues several small queries per article; fine at single-user
  scale, cacheable later if needed.
- "Used in a campaign" means *play references* (beats/sheets/statblocks), not
  every mention — a deliberate, useful definition. Documented so it's not
  mistaken for full-text mentions.
- New reference types (future entities) just add a case to the service.

## Alternatives considered
- **Nested per-type response**: richer typing but a bigger contract and rigid UI;
  the flat list is simpler and extensible.
- **Precomputed/stored backlinks**: premature; live aggregation is cheap here.
