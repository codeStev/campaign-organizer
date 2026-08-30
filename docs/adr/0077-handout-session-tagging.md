# 77. Optional session tagging for handouts

Date: 2026-08-30
Status: Accepted

## Context

The session prep packet (ADR-0036) aggregates everything needed to run a
session — beats, referenced articles, statblocks, maps, roll tables — onto
one printable document. Handouts (ADR-0070) were entirely disconnected from
it: a GM prepping a session's packet had to separately open the Handouts
page to find and print that session's props. ADR-0070 explicitly left room
for this kind of extension ("gives future handout features... a home
without re-parenting") without committing to it upfront.

## Decision

Add a nullable `session_id` FK to `handouts` — one handout tags at most one
session; null means general/unassigned. Not many-to-many: a GM who wants to
reuse a prop across sessions duplicates it or leaves it untagged. A join
table would support reuse more cleanly, but adds real complexity (a new
table, a different query shape, multi-select UI) for a case that's rare in
practice — most props (a specific letter, a specific poster) belong to one
night. Revisit only if reuse turns out to be common.

`SessionPacketService` (interchange context) now depends on `handouts`'
published `HandoutQueryPort.findBySession(sessionId)` — the same
published-port pattern it already uses for beats/articles/statblocks/maps/
tables from five other contexts (ADR-0050). `SessionPacketResponse` gains a
`handouts: PacketHandout[]` array; each entry carries raw Markdown `body`
(not pre-rendered HTML), since handouts already render client-side like the
rest of a packet's freeform text (session summary, GM notes, beat bodies) —
no reason to add a server render pass just for this one field.

**Validation**: `handouts` only carries a `worldId` (no `campaignId`), but
`sessionId` needs checking against a session that actually exists *in that
world*. `SessionQueryPort` (campaign context) only supported campaign-scoped
lookups, so it gains `findById(sessionId)` (the returned `SessionView`
already carries `campaignId`). A new `HandoutSessionExistsAdapter` in
`handouts.adapter.out.context` resolves the session's campaign via that new
method, then checks `CampaignQueryPort.existsInWorld(campaignId, worldId)`
— a two-hop check, but each hop is an existing published-port method. This
follows the same convention `ArcBeatCommandService` already uses for its
own cross-context id references (`ArticleExistsPort`, `StatblockExistsPort`,
`TableExistsPort`) rather than skipping validation for this one field.

**Still not linked into the wiki graph.** This is operational grouping for
packet assembly, not the kind of linking ADR-0070 declined — no backlinks,
no usage-panel entry, no `[[wiki-link]]` resolution. A handout's session tag
answers "which packet does this print with," nothing else.

## Consequences

- Migration `V28__handout_sessions.sql`: nullable `session_id` FK,
  `ON DELETE SET NULL` (deleting a session un-tags its handouts rather than
  deleting them — a prop can outlive the session it was written for).
- `docs/api/openapi.yaml`: `sessionId` (nullable) added to `Handout`/
  `HandoutRequest`; new `PacketHandout` schema; `SessionPacket.handouts`.
- `HandoutsView.tsx` gets a session picker (flattened across the world's
  campaigns, since handouts aren't scoped to one campaign); `SessionPacketView.tsx`
  gets a Handouts section, each rendered full-page with its own preset
  styling (`.print-map-section` wrapping, matching how every other packet
  section already forces a page break).
- No backend changes needed in `interchange.export` beyond remapping the
  new field on import — handouts already round-trip through world backups.
