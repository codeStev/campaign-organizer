# ADR-0041: Select specific statblocks to print

- Status: Accepted
- Date: 2026-08-15
- Amends: ADR-0037 (printable statblock cards)

## Context
ADR-0037 printed cards for the whole (campaign-filtered) list and explicitly
deferred per-card selection. In practice the owner wants to print only a chosen
subset — e.g. just tonight's encounter — not every statblock in the filter.

## Decision
Add a checkbox to each row in the statblocks panel. "🖨 Print cards" prints the
ticked statblocks; when nothing is ticked it still prints the whole filtered list
(prior behaviour). The button shows the count ("Print 3 cards"), a "Clear" action
resets the selection, and selection is intersected with the current list so a
campaign-filter change never prints a hidden statblock. Client-only; no backend or
contract change.

## Consequences
- The GM can assemble an arbitrary set of cards for a session without relying on
  campaign scoping.
- Selection is transient (not persisted) and scoped to what is currently listed.

## Alternatives considered
- **Selection inside the cards tab** (toggle cards after opening) — rejected;
  choosing before printing, in the list the GM is already scanning, is simpler.
