# ADR-0048: Per-layer pin icons

- Status: Accepted
- Date: 2026-08-15

## Context
Pins were coloured by layer and numbered (ADR-0045). The owner wanted pins to look
nicer and read faster by giving each layer a distinct **icon** (castle, mountain,
skull, …) rather than a bare number.

## Decision
Add an SVG icon library (`lucide-react`) and a curated registry of ~22
worldbuilding icons (`components/mapIcons.tsx`). Each map **layer** can be assigned
an icon, chosen from a per-layer dropdown in the maps sidebar; the choice is
persisted in `localStorage` alongside the layer colour.

- When a layer has an icon, its pins render that icon (white) inside the
  layer-coloured badge instead of the number; layers without an icon keep the
  number. This applies to the map editor (Leaflet markers + legend) and the direct
  map print (ADR-0047).
- For Leaflet's HTML `divIcon`, icons are rendered to static SVG markup
  (`renderToStaticMarkup`) and cached by key; in React contexts the icon component
  is rendered directly.

Icons are a client-side presentation preference (like colours), not shared world
data; no backend, contract, or schema change.

## Consequences
- Layers are identifiable at a glance by shape as well as colour; maps look more
  finished.
- Numbers are dropped for iconised layers, so on-map identification of a specific
  same-layer pin relies on labels/legend rather than a unique number — acceptable
  for the "prettier" goal.
- Adds one frontend dependency (`lucide-react`, MIT, tree-shaken to the icons
  used).
- Icons/colours live in `localStorage`, so they don't travel with the world
  export; can be promoted to persisted layer metadata if that's wanted.

## Alternatives considered
- **Emoji icons** — zero-dependency, but inconsistent cross-platform rendering and
  can't be tinted to the layer colour.
- **Per-pin icons** — finer but heavier UI; layer-level keeps one choice per group
  and matches how colours already work.
