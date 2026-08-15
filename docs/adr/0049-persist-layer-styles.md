# ADR-0049: Persist per-layer map styling on the world

- Status: Accepted
- Date: 2026-08-15
- Amends: ADR-0044 (layer colours), ADR-0048 (layer icons)

## Context
Per-layer map colours (ADR-0044) and icons (ADR-0048) were stored in the
browser's `localStorage`. That kept them off the server but meant styling did not
travel with the world's JSON export and was not shared across devices/browsers.
The owner asked to promote it to real world data.

## Decision
Store per-layer styling as world data. A `worlds.layer_styles` JSONB column (V21)
holds a map of **layer name → `{ color, icon }`** (`LayerStyle`). It is exposed
and edited via a dedicated sub-resource:

- `GET /worlds/{worldId}/layer-styles` → the style map.
- `PUT /worlds/{worldId}/layer-styles` → replace the map.

The map also rides on the `World` response and the world export bundle (the
export already serialises the world entity), so styling is portable. The maps UI
loads styles from the server and writes changes back on each colour/icon edit;
`localStorage` is no longer used for this.

## Consequences
- Layer colours and icons now export with the world and appear on any
  device/browser.
- Styling is keyed by layer *name*, shared across all maps in the world (a "cities"
  layer looks the same everywhere) — consistent with how layers were already
  treated.
- One new JSONB column and a small sub-resource; no per-layer entity needed.
- Existing browsers' `localStorage` styles are not migrated; they are simply
  re-set once on the server (a one-time, low-cost reset).

## Alternatives considered
- **A `map_layer_styles` table** (row per world+layer) — more schema and endpoints
  than a small, world-scoped JSON map warrants.
- **Keep it in `localStorage`** — rejected; the whole point was portability with
  the export and across devices.
