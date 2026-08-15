# ADR-0037: Printable statblock cards

- Status: Accepted
- Date: 2026-08-15

## Context
The owner runs sessions from paper (ADR-0035). Alongside the session packet
(ADR-0036), a common table aid is a set of compact **statblock cards** to keep
behind the GM screen — one card per creature, several to a page, cut apart.

## Decision
Add a client-only **statblock cards** print view (`StatblockCardsView`). A
"🖨 Print cards" action in the statblocks panel opens a print document that tiles
the currently listed statblocks as bordered (dashed cut-line) cards — name, a
two-column stat grid, and notes — then hands off to the browser's print /
Save-as-PDF.

- It prints exactly the panel's current list, so the existing **campaign filter**
  doubles as the card selection (e.g. print only this campaign's bosses).
- Cards use `break-inside: avoid` so a card never splits across a page, and tile
  with a CSS auto-fill grid.
- Reuses the print overlay/portal and `@media print` plumbing from ADR-0035; no
  backend, contract, or schema change (the statblock list already carries name,
  stats, and notes).

## Consequences
- One click produces cut-out reference cards matching whatever the GM has
  filtered to.
- Card selection is coarse (whatever the list shows); per-card multi-select could
  be added later if needed.
- Fidelity depends on the browser print engine, consistent with the other print
  views.

## Alternatives considered
- **A server-rendered card PDF** — unnecessary; the data is already on the client
  and browser Save-as-PDF suffices.
- **Multi-select checkboxes for exactly which cards to print** — deferred; the
  campaign filter covers the common case with less UI.
