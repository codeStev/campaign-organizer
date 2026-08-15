# ADR-0047: Direct map printing with options

- Status: Accepted
- Date: 2026-08-15

## Context
Maps could only be printed as part of the whole-world compendium (ADR-0035),
which shows every pin at a fixed size with no controls. The owner wanted to print
a single map on its own, tuned for the table — sized to the page, styled, and with
control over which pins show (e.g. a **players' copy** that hides locations they
haven't discovered).

## Decision
Add a **"🖨 Print map"** action to the map view that opens a dedicated
`MapPrintView` in its own tab (via `NewWindowPortal`, ADR-0038) with a
customization menu:

- **Scale** — a slider setting the printed map width (30–100% of the page).
- **Visual filter** — a CSS `filter` preset on the image (none, grayscale, sepia,
  parchment, blueprint, high contrast).
- **Pin filtering** — show/hide all pins, and per-layer include checkboxes plus an
  "(no layer)" toggle, so a layer of secret locations can be omitted from a
  players' copy.
- **Labels / legend** — toggle on-map labels (numbered badges otherwise) and the
  legend list.

Pins are colour-coded by layer and numbered; labels use the resolved
label/article-title fallback (ADR-0045). Client-only; no backend change.

## Consequences
- One map can be printed at the right size and style, and the same map yields a
  full GM copy or a filtered players' copy without any GM-only data model.
- Visual filters rely on the browser print engine honouring CSS `filter` (fine in
  Chromium, the owner's target).
- Pin filtering is per-layer; finer per-pin hiding would need explicit pin flags,
  deferred until needed.

## Alternatives considered
- **Server-rendered map export** — unnecessary; the image and pins are already on
  the client and browser print/PDF handles output.
- **A GM-only pin flag for the players' copy** — heavier; layer filtering already
  gives the "hide these locations" control (and layers are the natural grouping).
