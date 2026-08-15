# ADR-0045: Numbered pins, legend, and on-map labels

- Status: Accepted
- Date: 2026-08-15
- Amends: ADR-0044 (map pin UX)

## Context
With pins colour-coded by layer (ADR-0044), identifying a specific pin still meant
hovering each one for its tooltip. The owner wanted the print view's affordances
in the editor: numbered pins with a legend, and the option to show labels directly
on the map — plus a sensible label when a pin has none.

## Decision
- **Numbered pins + legend.** Each pin renders as a numbered badge (Leaflet
  `divIcon`) filled with its layer colour; a **legend** below the map lists the
  same numbers with colour swatch, label, and layer, and selecting a legend row
  selects the pin. Numbers follow the visible-pin order, matching the print view.
- **Optional on-map labels.** A "Labels" toggle in the map bar switches pin labels
  from hover tooltips to permanent labels drawn beside each pin.
- **Label fallback.** A pin's shown label is its own `label`, or — when unset but
  an article is linked — that **article's title**. Applied consistently to the
  map tooltips/labels, the legend, and the pin editor header.

Client-only; no backend, contract, or schema change.

## Consequences
- Pins are identifiable at a glance without hovering; the legend doubles as a
  quick pin picker.
- Linking an article is often enough to give a pin a meaningful name — no separate
  label needed.
- Numbering is positional (visible order), so hiding a layer renumbers the rest;
  acceptable and consistent with the print legend.

## Alternatives considered
- **Always-on labels** — clutters dense maps; a toggle keeps the numbered view
  available.
- **Persisting the label fallback into the pin's stored label** — rejected; it is
  derived at render so it stays in sync when the article title changes.
