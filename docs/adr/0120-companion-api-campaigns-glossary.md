# ADR-0120: Companion API: campaign list and campaign glossary

- Status: Accepted
- Date: 2026-10-23
- Extends: ADR-0109 (account privacy), ADR-0119 (API tokens)

## Context
DiscordWhispr records and transcribes voice sessions. It must (1) let the GM
pick which campaign a session belongs to, and (2) bias speech recognition and
its speech classifier with the proper nouns of exactly that campaign. A client
holding an API token (ADR-0119) knows an account, not a world, so it needs
account-level, campaign-addressed endpoints. Existing endpoints are all nested
under `/api/worlds/{worldId}/...`.

## Decision

### A new module: `interchange/companion`
Cross-context read composition belongs in `interchange` (like `packet` and
`overview`), not in a core context. The module owns no domain rules beyond the
glossary selection rule below; it reads other contexts only through their
`application.port.published` ports. All its endpoints live under
`/api/companion/**`, the one namespace API tokens may reach, which keeps the
token allowlist (ADR-0119) a handful of path rules.

### Endpoints
| endpoint | scope |
|---|---|
| `GET /api/companion/campaigns` | `campaigns:read` |
| `GET /api/companion/campaigns/{campaignId}/glossary` | `glossary:read` |

Both also work for a normal full-MFA login. Neither takes a world id.

**Campaign list**: every campaign of every world the caller owns
(`WorldQueryPort.findByOwner` then `CampaignQueryPort.findByWorld`), each with
id, name, status, world id and world name. Ownership is resolved explicitly in
the application service, not left to row-level security alone.

**Glossary**: for one campaign, owned by the caller (otherwise 404, same
masking as ADR-0109):
```
{ campaignId, campaignName, terms: [ { name, kind, aliases: [string] } ] }
```
Names only: no article bodies, no ids, no notes. Spoiler-bearing text never
leaves the organizer.

### What is relevant (the selection rule)
A term belongs to the glossary when it is tied to the campaign:
1. **Articles linked from the campaign's beats** (arcs of the campaign, then
   `ArcBeatQueryPort.linkedArticleIdsByArcs`), **plus articles referenced by
   the campaign's character sheets and statblocks** (`articleId`).
   Only articles whose template is `CHARACTER`, `LOCATION`, `ORGANIZATION`,
   `SPECIES` or `ITEM`. `GENERIC` and `EVENT` articles add noise, not proper
   nouns. Title plus all aliases (ADR-0116) are sent; the kind is the
   lowercased template.
2. **Character sheets of the campaign** (`campaignId` set): name, kind `character`.
3. **Statblocks of the campaign**: those with `campaignId` set plus those
   linked by its beats (the same union as the existing "statblocks relevant to
   a campaign" rule): name, kind `creature`.

Terms are de-duplicated case-insensitively per kind and sorted by name.
Nothing from the rest of the world is included, so the client never receives
content unrelated to the campaign. Player names are not part of the glossary.

### Changes to other contexts (published ports only)
- `worldbuilding.wiki`: `ArticleQueryPort.findSummariesByIdsInWorld(worldId, ids)`
  (body-free) and a new `ArticleAliasQueryPort.findByArticles(worldId, ids)`.
- Nothing else; campaign, arc, sheet, statblock and world ports already exist.

### Token access
`SecurityConfig` rules for the two paths accept either the full-MFA login or
the matching `SCOPE_*` authority. Added only once ADR-0119 is on the base
branch.

## Consequences
- The recorder can offer campaign autocomplete and fetch a glossary with a
  token that cannot touch anything else.
- A campaign with no linked beats, sheets or statblocks yields an empty
  glossary. That is intended; the GM links material to the campaign to feed it.
- Per-request cost is a handful of indexed reads; no caching.

## Alternatives considered
- **Whole-world glossary**: simplest, but leaks every NPC and secret location
  of unrelated campaigns into the transcript pipeline. Rejected; an optional
  `includeWorld` switch can be added later if wanted.
- **Endpoints under `/api/worlds/{worldId}/...`**: the client would need the
  world id and the world-access gate would also have to learn about tokens.
- **Putting this in `campaign`**: it composes worlds, articles, sheets and
  statblocks; it is orchestration, which this repo keeps in `interchange`.
