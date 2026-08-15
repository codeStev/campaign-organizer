# ADR-0038: Print views open in a separate tab

- Status: Accepted
- Date: 2026-08-15

## Context
The print/PDF views (world compendium ADR-0035, session packet ADR-0036,
statblock cards ADR-0037) were rendered as a full-screen overlay portalled into
the app's own `<body>`, and printing hid `#root`. That overlay took over the
current tab: opening a packet meant leaving the app view and clicking "Close" to
get back, which the owner found disruptive to navigation.

## Decision
Render every print view into a **separate browser tab** via a shared
`NewWindowPortal` component. On mount it `window.open()`s a blank same-origin tab,
clones the app's stylesheets (`<link rel="stylesheet">` by resolved absolute URL,
`<style>` by node clone) so the `.print-*` styles apply, mounts a container, and
`createPortal`s the view's React tree into it. Closing the tab calls `onClose`, so
the triggering component resets its state.

`PrintView`, `SessionPacketView`, and `StatblockCardsView` now render their
existing toolbar + document markup inside `NewWindowPortal` instead of an overlay.
The `@media print` rules that hid `#root` and repositioned the overlay are gone;
the new tab is nothing but the document, so printing it "just works".

## Consequences
- The app tab is untouched — the user keeps their place and navigation while a
  packet/compendium/cards tab sits alongside for printing or PDF export.
- Because the print tree lives in another document, it is isolated from app
  layout entirely; no more visibility/`#root` hacks.
- Opening requires a user gesture (all triggers are button clicks) so popup
  blockers allow it; if a popup is blocked, the portal calls `onClose` and nothing
  renders. A future enhancement could surface a "popup blocked" hint.
- Stylesheets are snapshot-cloned at open time; that is fine for a static print
  document.

## Alternatives considered
- **Keep the same-tab overlay** — rejected; it was the exact navigation problem
  reported.
- **A dedicated print route opened by URL** — the app has no router and would
  re-bootstrap (and re-authenticate) on a fresh load; portaling the already-built
  React tree into a new window is lighter and keeps a single source of truth.
