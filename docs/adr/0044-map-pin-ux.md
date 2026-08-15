# ADR-0044: Map pin UX — inline editing and layer colours

- Status: Accepted
- Date: 2026-08-15

## Context
The map pinning flow (ADR-0018) had three usability problems reported by the
owner:

1. All pins were the same colour, so layers weren't visually distinguishable.
2. Creating a pin used two back-to-back `window.prompt()` dialogs (label, then
   layer). Browsers add a "Prevent this page from creating additional dialogs"
   checkbox to the *second* dialog in a burst; ticking it suppressed every later
   prompt, so pins could no longer be created — the confusing "weird checkbox".
3. Linking an article to a pin didn't reliably stick.

## Decision
Rework the maps UI (no schema/contract change).

- **Colour by layer.** Each layer gets a stable colour from a fixed palette
  (hashed by name), adjustable per layer via a colour picker; overrides persist in
  `localStorage`. `MapCanvas` fills each pin with its layer colour (default violet
  for layerless pins); the selected pin keeps a bright ring so the colour still
  shows.
- **No prompts.** Clicking the map creates a pin immediately at that point and
  selects it. Label, layer, and linked article are then edited in an inline
  **pin editor** panel (layer field backed by a `<datalist>` of existing layers)
  and written together with an explicit **Save**. This removes the browser-dialog
  problem entirely and makes linking deterministic.
- **Clearer layer panel.** Each layer row shows its colour swatch, a visibility
  checkbox, and a one-line hint.

## Consequences
- Layers are visually separable and user-tunable; pin creation is a single click;
  article links save as part of an explicit form submit.
- Layer colours are a client-side preference (localStorage), not shared world
  data — acceptable for a single-user app; can be promoted to persisted layer
  metadata later if needed.
- Map creation still uses one name prompt (a single dialog, not the problematic
  burst).

## Alternatives considered
- **Keep prompts but collapse to one** — still dialog-based and can't edit later;
  an inline editor is strictly better.
- **Store colours on the server** — unnecessary for one user; localStorage keeps
  it simple and instant.
