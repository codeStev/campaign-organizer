# ADR-0046: Session packet includes linked maps

- Status: Accepted
- Date: 2026-08-15
- Amends: ADR-0036 (session prep packet)

## Context
The session packet gathered a session's beats, their referenced articles, and
statblocks — but not the **maps** those places appear on. A GM prepping a session
about "Phandalin" wants the map showing Phandalin in the same printout.

## Decision
The packet now includes maps reachable from the session: for each article
referenced by the session's beats, any map with a **pin linking that article** is
included. Each `PacketMap` carries the map name, image URL, and its pins as
`PacketPin{ x, y, label }`, with the label resolved server-side (the pin's own
label, or its linked article's title as fallback — consistent with ADR-0045).
`SessionPacketView` renders each map as an annotated image (numbered markers +
legend), reusing the world-print map rendering.

## Consequences
- A session's maps print alongside its lore, so the GM has the geography on paper
  too.
- A map is pulled in transitively (beat → article → pin → map); a location only
  needs to be pinned once to surface in every session that uses it.
- Maps are shown in full (all their pins) for GM prep; player-facing pin filtering
  is handled by the dedicated map print (ADR-0047), not the packet.

## Alternatives considered
- **Only include pins that link beat articles** (hide other pins) — rejected for
  the GM packet; the whole map is more useful for prep. Filtering is the map
  print's job.
- **Require an explicit map↔session link** — unnecessary; the pin→article→beat
  chain already expresses relevance.
