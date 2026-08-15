# ADR-0036: Session prep packet

- Status: Accepted
- Date: 2026-08-15

## Context
The owner preps in the app but runs sessions from paper (ADR-0035). The world/
campaign print view is a good compendium, but for a given night the GM wants a
focused hand-out: just the beats scheduled into *this* session, the NPCs/locations
those beats reference, and the campaign's statblocks — not the whole world. The
data to assemble this is spread across arcs, beats (which carry a `sessionId` and
linked `articleIds`), articles, and campaign-scoped statblocks.

## Decision
Add a backend aggregation endpoint and a dedicated print view.

- **Endpoint.** `GET /worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/packet`
  returns a `SessionPacket`: the `Session`, the `campaignName`, the beats scheduled
  into that session (in play order, each with its `arcTitle` for context and its
  linked `articleIds`), the articles those beats reference (de-duplicated,
  first-seen order, rendered to `bodyHtml` server-side so images and `[[wiki-links]]`
  resolve), and the campaign's statblocks. Implemented in `SessionPacketService`;
  beats are found via a new `ArcBeatRepository.findBySessionId…` query.
- **Why server-side aggregation.** Gathering a session's beats requires scanning
  every arc's beats for the `sessionId`, then fetching and rendering each
  referenced article — an N+1+M orchestration that is simpler, cheaper, and
  testable as one endpoint than done from the client.
- **Print view.** `SessionPacketView` renders the packet as a print document —
  cover (session number/title, campaign, date), an overview (summary + GM notes),
  a beat checklist, the referenced articles in full, and statblocks — reusing the
  print overlay/portal and `@media print` styling from ADR-0035, and delegating to
  the browser's print / Save-as-PDF. Triggered by a "🖨 Packet" button per session
  in the session log.

## Consequences
- One click turns a session into a physical prep sheet with everything needed at
  the table, building directly on the existing beat→session links (ADR-0031) and
  campaign-scoped statblocks (ADR-0032).
- The packet reflects what beats are *scheduled into* the session; beats with no
  `sessionId` are intentionally excluded. Unlinked lore is still reachable via the
  world/campaign print view.
- A new small cross-feature service reaches into the wiki (`AutoLinker`,
  `ArticleRepository`) and statblock repositories; acceptable for an aggregation
  read model.

## Alternatives considered
- **Client-side assembly** from existing list endpoints — rejected for the N+1+M
  fan-out and the need to render article bodies (only available per-article).
- **Extending the world/campaign print view** (ADR-0035) with a session scope —
  rejected: the packet's shape (beats, overview, statblocks) differs enough that a
  purpose-built view and endpoint are clearer than overloading the compendium.
