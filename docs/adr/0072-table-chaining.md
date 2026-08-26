# 72. Roll-table chaining (FR-41)

Date: 2026-08-26
Status: Accepted

## Context

FR-40's roll tables and card decks are flat randomizers. Real prep often wants
one step further: a table outcome ("Ambush!") should be able to invoke another
table ("roll on Loot") or draw from a deck, recursively — and whatever a chain
reaches must appear in session packets and printouts without breaking FR-40's
print-once rule.

## Decision

**Rows and cards gain two reference lists.** `RollTableEntry` carries
`nestedTableIds` + `nestedDeckIds`; `DeckCard` mirrors them. Any table can
chain any deck and vice versa, so the graph is heterogeneous and may loop.

**Validation at save time is deliberately shallow.** The services reject only
what is unambiguous at write time:

- self-nesting (`A`'s row referencing `A`) → 400;
- nested ids that do not exist in the same world (checked through the other
  aggregate's repository port — `RollTableService` injects
  `CardDeckRepositoryPort`, `CardDeckService` injects `RollTableRepositoryPort`)
  → 400.

Indirect cycles stay legal on disk. A→B plus B→A is a legitimate stored state
(each save was individually valid) and rejecting it would require whole-graph
analysis on every edit for little benefit.

**Cycle cutting happens at resolution time.** Every place that walks the chain
graph carries visited sets:

- *Session packet* (`SessionPacketService`): the beat-seeded tables/decks feed
  a two-queue BFS; each id loads once, its rows' nested ids enqueue both
  directions, already-visited ids are skipped. First-seen order preserved;
  cycles terminate naturally.
- *Standalone table/deck print* (frontend): same closure over the loaded
  lists, appended sections after the printed root.
- *Live roll/draw* (frontend): recursive sub-rollers with a depth cap
  (`MAX_CHAIN_DEPTH`), because a cyclic graph would otherwise recurse forever.
- *Compendium print* needs no closure — it prints every table and deck of the
  world anyway.
- *Export/import*: entry/card bodies were already rewritten by `IdRemap`;
  the nested id lists ride along via the new `IdRemap.all(...)`, trusting the
  bundle like every other reference.

**Storage stays JSONB-shaped.** The nested lists live inside the existing
entry/card JSON payloads (new nullable array keys); old rows read back as
empty lists via compact-constructor normalization — no migration needed.

## Consequences

- A deleted chained target leaves a dangling id; resolvers skip it silently
  (`findByIdInWorld().ifPresent` / frontend "deleted" note). Consistency
  report (FR-43) is the place to surface such dangling chains later.
- Packet size grows with reachability, not with explicit references; that is
  the point, but a pathological all-chained world prints everything. Accept-
  able for a single-user instance.
- The depth cap only affects the interactive roller's rendering, never what
  is stored or printed.
