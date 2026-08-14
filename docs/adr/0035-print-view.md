# ADR-0035: Print / PDF view via the browser

- Status: Accepted
- Date: 2026-08-15

## Context
The owner's primary workflow is to **prep in the app, then print everything on
paper to run sessions physically** — screen-sharing is not used. Until now the
only export was a JSON world bundle (ADR-0022 area), which is a backup/interop
format, not something you hand out at the table. There was no way to get a
readable, black-on-white document of a world's lore, and no way to print maps
with their pins.

## Decision
Add a client-only **Print / PDF view**: a full-screen overlay
(`pages/PrintView.tsx`) that renders a clean print document and delegates to the
browser's native print / *Save as PDF* via `window.print()`. No backend or
contract change — it composes existing endpoints.

- **Content.** A cover page (world/campaign name + date), an optional Contents
  list, every article rendered from its server-side `bodyHtml` (so embedded
  images and resolved `[[wiki-links]]` come through — links are styled as plain
  emphasized text on paper), and, for whole-world scope, each map as its image
  with numbered pin markers overlaid plus a numbered legend.
- **Scope.** Whole world, or a single campaign — the campaign option reuses the
  existing `GET /articles?campaignId=` usage filter (ADR-0033) so a "print this
  campaign's material" packet is one selection. Articles are fetched per-id for
  their rendered bodies and sorted A–Z into a stable booklet order.
- **Print isolation.** The overlay is rendered through a React portal into
  `<body>`; the print stylesheet hides `#root` entirely and lets only the
  portalled document paginate, with `break-before: page` between articles/maps.

## Consequences
- The user gets physical handouts and PDFs for free, using the browser's mature
  print engine and the OS print dialog (paper size, margins, printer).
- No new server code, dependency, or PDF library to maintain; layout is plain CSS
  and iterates quickly.
- Building a packet is an N+1 fetch (list, then each article). Fine at
  single-user scale; can be replaced with one aggregating endpoint if it ever
  gets slow.
- Print fidelity depends on the browser. Acceptable given it is the owner's own
  machine; page breaks and colors are tuned for Chromium's print engine.

## Alternatives considered
- **Server-side PDF generation** (headless render or a PDF library) — rejected as
  heavier to build and maintain, less flexible, and unnecessary when browser
  *Save as PDF* already produces high-quality output.
- **A dedicated print route** — the app has no router; a portalled overlay
  toggled by state matches the existing architecture (cf. the command palette,
  ADR-0034).
