# ADR-0086: Fold the cheat sheet into the session packet

- Status: Accepted
- Date: 2026-09-01
- Amends: ADR-0036 (session prep packet), ADR-0071 (session cheat sheet)

## Context
The session packet (ADR-0036) already auto-assembles a session's beats,
their referenced articles/maps/statblocks, linked roll tables and card
decks, tagged handouts, and the campaign's clocks into one printable
document with a GM-prunable include/exclude checkbox tree. The cheat sheet
(ADR-0071) is a separate, hand-curated per-session document with its own
standalone print button. ADR-0071 explicitly decided *"the cheat sheet does
not appear in the session packet... it prints standalone, keeping both
surfaces predictable."* In practice this means printing "everything for
tonight" is two separate print jobs. The issue asks to fold the cheat
sheet's content in as another node in the packet's existing include-tree,
default **included** (opt-out, matching every other section), which is
exactly the predictability ADR-0071 was protecting — the two documents stay
independently understandable, the packet just also offers to include one of
them.

## Decision
- **No new aggregate, no new ports.** `CheatSheet` already publishes
  `CheatSheetQueryPort.findBySession`; `SessionPacketService` injects it
  and resolves fragments through the same out-ports it already holds for
  other sections (`StatblockQueryPort`, `RollTableQueryPort`,
  `CardDeckQueryPort`, `ArticleRenderPort`) — no new out-port is needed.
- **Server-side resolution**, not the client-driven full-list-fetch
  `CheatSheetView.tsx` uses for its own standalone print. This keeps
  ADR-0071's "references render live from their sources at print time"
  rule intact — the packet is already recomputed fresh on every request
  (`@Transactional(readOnly = true)`, nothing cached), so resolving
  fragments there is no less live than the standalone view.
- **Reuse the packet's own resolved shapes.** A `TABLE_ROW` fragment
  resolves to the same `PacketRollTableEntry` the packet's Tables section
  already produces (body pre-rendered through `articleRenderer`); a
  `DECK_CARD` fragment resolves to the same `PacketDeckCard`. The frontend
  reuses whatever already renders those shapes elsewhere in the packet
  instead of a second rendering path.
- **STATBLOCK fragments resolve to the packet's existing `StatblockView`**
  but render in the cheat sheet's own condensed one-line style, not the
  packet's full statblock block — the cheat sheet's entire reason for
  existing is a dense, glanceable page (ADR-0071's "must print as one dense
  page"), and folding it into the packet shouldn't lose that. The condensed
  formatting helpers (`statblockLine`, `entryRange`, `cardLabel`), private
  to `CheatSheetView.tsx` until now, move to a shared
  `frontend/src/lib/cheatSheetDisplay.ts` so both surfaces use the same
  formatting.
- **Dangling references stay dangling, visibly** — an unresolvable fragment
  maps to a `PacketCheatSheetFragment` with a `missing` flag rather than
  being dropped or erroring the whole packet, the same trade-off ADR-0071
  already accepted for the standalone view.
- **One include-tree node for the whole sheet, not one per fragment.**
  Every other packet section's tree nodes are per-item because those items
  are independently meaningful elsewhere in the app (a beat, a map, a
  clock). A cheat sheet is a single ordered, hand-curated document — one
  node the GM includes or excludes wholesale matches both the issue's own
  phrasing ("another node") and how the document is actually used.
- **Default included.** Not present in the frontend's `excludedIds` set
  initially, same as every other section — full control, opt-out not
  opt-in, per the issue.
- **ADR-0071's "does not appear in the session packet" sentence is
  superseded** by this decision; everything else in ADR-0071 (aggregate
  shape, validation, standalone editor/print) is unchanged.

## Consequences
- The packet gains a `cheatSheet` field (nullable — absent when the session
  has no saved sheet, or an empty one) rather than every session
  unconditionally getting an empty node to exclude.
- The standalone cheat sheet editor/print (ADR-0071) is untouched in
  behavior; the only change there is importing shared formatting helpers
  instead of defining them locally.
- A future fifth fragment kind still only touches what ADR-0071 already
  scoped (domain switch, web DTO, mapper, one out-port) plus one more
  resolution branch in `SessionPacketService` and one more render case in
  the shared display helpers — not a second, independent extension point.

## Alternatives considered
- **One tree node per fragment**: more granular, but a cheat sheet's
  fragments are curated as a single ordered unit by design (ADR-0071); a GM
  wanting less of it edits the sheet itself, not the packet's selection.
- **Client-side merge** (fetch both `GET .../packet` and
  `GET .../cheat-sheet`, combine in the frontend): rejected for the same
  reason ADR-0036 chose server-side aggregation originally — reference
  resolution (statblock/table-row/deck-card lookups) belongs behind the
  same published ports the rest of the packet already uses, not duplicated
  client-side against full entity lists.
- **Reuse the packet's full statblock rendering for STATBLOCK fragments**
  instead of extracting the condensed helpers: simpler, but defeats the
  cheat sheet's one-dense-page design goal — rejected.
