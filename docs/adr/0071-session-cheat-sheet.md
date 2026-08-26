# ADR-0071: Session cheat sheet

- Status: Accepted
- Date: 2026-08-25

## Context
FR-37 asks for a **condensed one-page GM cheat sheet per session**: the few
things the GM actually glances at during play, on paper next to the screen.
It is *not* another document that assembles content (the session packet,
ADR-0036, already does that) — it is an **authored list** the GM curates by
hand, in a deliberate order.

Two content kinds emerged from the requirements work:

- **Freeform snippets** ("check the harbor door first", the bribed guard's
  name).
- **References to existing content**, so the sheet never goes stale when the
  source changes: a statblock, one specific roll-table row, one specific deck
  card (FR-40 introduced those randomizers).

The sheet must print as one dense page, ordered exactly as composed.

## Decision
- **Lives inside the existing `campaign` context's session ring**, not a new
  bounded context: a cheat sheet has no life outside its session — it is
  session data like summary and notes, just structured. Aggregate:
  `CheatSheet` (one per session) holding an ordered `List<CheatSheetFragment>`.
- **JSONB whole-aggregate persistence** (`cheat_sheets`, V27), following the
  whiteboard (ADR-0027) and roll-table precedents: fragments are always
  loaded/saved together, never queried individually. One row per session,
  `session_id UNIQUE`, `ON DELETE CASCADE`.
- **Four fragment types** with type-specific required fields, validated in the
  domain (`CheatSheetFragment` canonical constructor): `FREEFORM` needs text;
  `STATBLOCK` needs `statblockId`; `TABLE_ROW` needs `tableId` + `entryId`;
  `DECK_CARD` needs `deckId` + `cardId`.
- **Deeper reference validation than beats**: because a fragment points at one
  row/card (not just an aggregate), three dedicated out-ports
  (`StatblockExistsPort`, `TableEntryExistsPort`, `DeckCardExistsPort`)
  verify existence *in this world* on every save; unknown fragment types are
  rejected with the offending index. References render live from their sources
  at view/print time — the sheet stores ids only, so edits upstream show up on
  the next printout.
- **Singleton-per-session REST semantics** under
  `/worlds/{w}/campaigns/{c}/sessions/{s}/cheat-sheet`: GET returns a sheet
  with `id: null` and no fragments before the first PUT (so clients render an
  editor from one call), PUT upserts wholesale (the whole ordered list is the
  payload), DELETE removes idempotently. An empty fragment list is legitimate
  — it means "cleared".
- **Import trusts export** (ADR-0061 rule): imported sheets reconstitute
  fragments verbatim without re-validating references — the bundle was
  validated when exported, and import order cannot be made to match arbitrary
  reference graphs. Only the sheet id and session id are remapped.
- **Frontend**: a builder panel reachable from each session in the session log
  ("📋 Cheat sheet"), with kind-aware add controls (dependent selects for
  table→row and deck→card), ↑/↓ reordering, explicit Save, and a print window
  rendering the numbered, dense one-page sheet via the standard
  NewWindowPortal pattern (ADR-0038).

## Consequences
- A saved sheet can still dangle if its referenced table row or card is later
  deleted — the same trade-off beats accept; the UI renders a visible
  "Missing …" marker instead of failing.
- The cheat sheet does **not** appear in the session packet (that document
  assembles itself); it prints standalone, keeping both surfaces predictable.
- Adding a fifth fragment kind later touches exactly: domain switch, web DTO,
  mapper, one out-port if it references content.

## References
- [FR-37](../requirements.md)
- ADR-0036 (session packet), ADR-0038 (print windows), ADR-0061 (import),
  ADR-0066 (roll tables & card decks)
