# ADR-0043: Reference statblocks from beats

- Status: Accepted
- Date: 2026-08-15

## Context
Beats can already reference multiple articles (ADR-0032). But not every combatant
deserves a wiki article — "goblin spearman", "goblin archer", "goblin wizard" are
just statblocks. The owner wants to attach statblocks to a beat directly, and to
have that attachment implicitly make a statblock **relevant to a campaign** so a
shared (non-campaign-scoped) statblock still prints with that campaign's material.

## Decision
Add a many-to-many between beats and statblocks, mirroring `beat_articles`.

- **Schema/model.** New `beat_statblocks` join table (V20, both FKs
  `ON DELETE CASCADE`); `ArcBeat.statblockIds` as an EAGER `@ElementCollection`.
  `BeatRequest`/`BeatResponse` gain `statblockIds`; the beat editor links them via
  a dropdown + removable chips and shows them on the beat row.
- **Validation.** Each referenced statblock must belong to the world (400
  otherwise), like article links.
- **Implicit campaign relevance.** Statblocks referenced by any beat in a
  campaign's arcs count as belonging to that campaign for read purposes:
  - `GET /statblocks?campaignId=` now returns campaign-scoped statblocks **plus**
    statblocks referenced by that campaign's beats (deduped). This makes "print
    all relevant statblocks for a campaign" include shared ones.
  - The session packet (ADR-0036) lists statblocks referenced by the session's
    own beats first, then the campaign-scoped ones.

New repository queries: `findLinkedStatblockIdsByArcIds` and
`findLinkedStatblockIdsBySessionId`.

## Consequences
- Encounters can be built from reusable shared statblocks without creating throw-
  away articles or duplicating a statblock per campaign.
- A statblock's campaign relevance can now come from *either* an explicit
  `campaignId` (ADR-0032) *or* a beat reference — the campaign filter and packet
  treat both as relevant.
- Deleting a statblock or beat cleans up its links via cascade; the beat/statblock
  itself is unaffected.

## Alternatives considered
- **Only explicit `campaignId` scoping** (status quo) — forces duplicating shared
  monsters per campaign or manually re-scoping; rejected.
- **A dedicated encounter entity** — heavier than needed; beats already are the
  per-session unit and now carry their combatants.
