# ADR-0118: Session packet laid out for use at the table

- Status: Accepted
- Date: 2026-10-02
- Amends: ADR-0036 (session prep packet), ADR-0086 (cheat sheet in packet)

## Context
After playing a planned session from the packet, the GM found it hard to
locate information mid-game. The packet was a flat sequence (overview, beats,
articles, maps, statblocks, tables, decks, handouts, clocks, cheat sheet)
with no contents page and no running header. Almost every block forced a page
break, so a beat and the material it needs sat many pages apart, and
statblocks were a loose grid of label/value pairs rather than a table.

## Decision
- **Hybrid structure.** A front sheet (title, summary, GM notes, a beat
  checklist with a "Needs:" line per beat, and a contents list), then
  lettered sections: cheat sheet, beats in play order, articles grouped by
  template, statblocks, tables and decks, maps, handouts, clocks. Letters are
  assigned only to sections that have content after the include-tree filter.
- **The cheat sheet moves to the front** (after the front sheet) because it is
  the quick-lookup material. ADR-0086's decision to include it is unchanged.
- **Navigation aids.** The contents list carries page numbers and each
  section has a running page header ("D · Statblocks"). Both rely on CSS
  paged-media features whose browser support is uneven; the contents falls
  back to section letters without page numbers where unsupported.
- **Fewer page breaks.** A new page starts only at a section start and for
  maps and handouts. Items inside a section flow and avoid breaking inside.
- **Statblocks are real tables** with a shaded name row, template sections as
  sub-header rows, a four-column label/value grid for short fields and
  full-width rows for long text, packed two per row.
- **Contract:** `PacketBeat` gains `statblockIds`, `tableIds`, `deckIds` so
  the front sheet can link beats to their material directly. The server
  still does no layout; ordering and grouping are client-side.

## Consequences
- The packet's document order differs from ADR-0036; the include tree and the
  GM-notes toggle are unchanged.
- Encounters linked to beats are still not in the packet (separate follow-up).

## Alternatives considered
- **Play order only** (all material inline under each beat): duplicates or
  scatters shared items and makes lookup by name harder.
- **Keep the flat layout and add a contents page**: does not fix the
  distance between a beat and its material or the page count.
