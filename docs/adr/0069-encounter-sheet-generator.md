# 69. Encounter sheet generator (FR-44)

Date: 2026-08-26
Status: Accepted

## Context

Combat at the table needs a tracking sheet: who's in the fight, initiative
order, and a way to tick off HP. The statblocks already exist; building the
sheet by hand each fight is busywork. FR-44 asked for assembly of picked
statblocks (and optionally PC sheets) into a printable combat tracker —
explicitly "pure assembly over existing content", in keeping with the
print-first workflow.

## Decision

A **client-side builder + print view**, no new API contract and no
persistence:

- Entry point is an "⚔ Encounter" action beside the existing card-printing
  bar in the statblocks panel; it receives the same selection semantics
  (ticked statblocks, or the whole filtered list).
- `EncounterSheetView` (ADR-0038 portal pattern) has two zones:
  - **Screen-only staging**: per statblock, a quantity (0–20) and an
    editable max HP. Max HP is auto-prefilled by looking for a numeric value
    under an HP-like key (`hp`, `max_hp`, "Hit Points"); the GM can overwrite
    or leave blank. PC sheets are opt-in via checkbox.
  - **The printed tracker**: one row per generated combatant ("Goblin 1",
    "Goblin 2"…), columns for Init (blank), Combatant, HP as groups of ten
    tick-boxes capped at sixty plus `/max` (blank `__/__` when unknown), and
    Key stats — up to six non-narrative template entries rendered as
    "AC 13 · Speed 30 ft".
- Quantities expand client-side; nothing about encounters is stored — the
  sheet is disposable like paper.

## Consequences

- HP detection is heuristic and template-dependent by nature; the editable
  prefill input makes wrong-or-missing detection harmless.
- Because nothing persists, re-building an encounter means re-picking —
  acceptable for ad-hoc fights; if recurring encounters are wanted later,
  that is a new aggregate with its own ADR.
- The sixty-box cap keeps rows to one line for most creatures; larger pools
  still print via the `/max` figure and can be tracked numerically.
