# ADR-0063: shadcn/ui (Radix + Tailwind v4) as the component foundation

- Status: Accepted
- Date: 2026-08-25

## Context
The frontend (ADR-0002) has always been plain React + hand-written CSS: 32
components, all built from raw `<button>`/`<input>`/`<select>` elements,
styled entirely through one ~1900-line global `index.css` of bare-tag and
BEM-ish class selectors, with a single hardcoded dark palette (ADR-0062's
`color-scheme: dark` fix leaned explicitly on "the app is hard-coded dark,
only one theme"). Keeping that hand-rolled component styling consistent, and
retheming it, both meant touching dozens of one-off selectors by hand.

The owner wants to adopt [shadcn/ui](https://ui.shadcn.com) — Radix UI
primitives (accessible, unstyled behavior) plus Tailwind CSS utility classes
(the look), generated as owned source files rather than an installed
package — to make the component layer easier to keep consistent, and to
open up real theming flexibility. shadcn's component source is *copied*
into the repo (`src/components/ui/*.tsx`) by its CLI, not pulled in as a
versioned dependency: "keeping up to date" means periodically re-running
`shadcn add <component>` and diffing, not an automatic bump — this ADR
adopts it for consistency and Radix's accessibility baseline, not for
automatic upstream sync.

Two scope decisions were made before implementation (brainstormed in-session,
not re-litigated here):
- **Migration scope: foundation + core primitives, app-wide.** Convert the
  highest-reuse pieces (Button, Input/Textarea, Select, Dialog, Tabs,
  Checkbox) everywhere they're used. Leave print views (`PrintView`,
  `MapPrintView`, `StatblockCardsView` — deliberately paper-styled,
  independent of app theme per the print-first workflow), the Leaflet map,
  the whiteboard canvas, and the relationship-graph SVG untouched — they're
  layout/rendering surfaces, not chrome.
- **Theming scope: a real light/dark toggle now**, not just a CSS-variable
  refactor of the existing dark palette. This supersedes the "single dark
  theme" premise of ADR-0062's `color-scheme` fix (that ADR's Pointer Events
  decision is unaffected).

## Decision
- **Tailwind v4** via `@tailwindcss/vite` (no separate PostCSS config needed)
  plus a `@/*` path alias (`tsconfig.json` + `vite.config.ts`), both
  required by the shadcn CLI.
- **shadcn CLI** (`base: radix`, `template: vite`) generates components into
  `src/components/ui/*.tsx`, using Radix primitives + `class-variance-authority`
  + `tailwind-merge` (via a `cn()` helper in `src/lib/utils.ts`).
- **Design tokens**: `index.css` defines the app's actual brand palette as
  CSS custom properties — `:root` (light) and `.dark` (the app's existing
  hand-picked dark colors, e.g. `--background: #14121a`, `--primary:
  #6d54c9`) — rather than the CLI's generic neutral-gray "Nova" preset
  defaults, which were overwritten. A `@theme inline` block maps these to
  Tailwind's `--color-*` namespace so utilities like `bg-primary` resolve.
  shadcn's own `--accent`/`--accent-foreground` (a neutral hover-highlight
  surface, distinct from the brand color) is kept separate from the app's
  existing brand-purple `--accent` variable, which is renamed to `--primary`
  throughout `index.css` (27 call sites) to avoid the two colliding.
- **Existing hardcoded hex values retired in favor of tokens**, for the
  base/shared chrome only: `body`, `.card`, borders, muted/error text,
  `.link-button`, editor toolbar, etc. — the surfaces used everywhere, so the
  toggle looks coherent at a glance. Map pins, whiteboard nodes, the
  relationship graph, diff-view colors, and all print CSS keep their
  original hardcoded values (out of scope; print in particular must stay
  paper-styled regardless of app theme).
- **Theme toggle**: a small hand-rolled `ThemeProvider` (React context +
  `localStorage` + toggling a `.dark` class on `<html>`) rather than
  `next-themes` — that package's value is avoiding SSR/hydration flash,
  which doesn't apply to this Vite CSR SPA. Defaults to dark on first visit,
  matching the app's existing look; a toggle button sits next to "Log out"
  in the header.
- **No Geist font**: the CLI's "Nova" preset pulled in `@fontsource-variable/geist`
  by default; removed, since no font change was requested — the app keeps
  Tailwind's default `font-sans` stack (effectively `system-ui`).

## Consequences
- New components (and future primitive conversions) get Radix's accessibility
  behavior (focus management, ARIA, keyboard nav) for free, and a real
  light/dark toggle works across the app's shared chrome immediately.
- The actual Button/Input/Select/Dialog/Tabs/Checkbox call-site conversions
  across the 32 existing components are follow-up work on this branch, done
  incrementally (one primitive, one commit) rather than in a single pass.
- `index.css` still carries ~1900 lines of bespoke CSS for everything outside
  the converted primitives (maps, whiteboard, print, sheet grids, etc.) —
  this is an intentionally incomplete migration, not a regression; those
  views were explicitly kept out of scope.
- Two dependencies removed as unnecessary for a Vite SPA: `next-themes`
  (never added) and `@fontsource-variable/geist` (added by the CLI, then
  removed).

## Alternatives considered
- **CSS-variable refactor only, no toggle**: simpler, but the owner
  explicitly wants toggleable theming, not just easier maintenance of one
  theme.
- **A different headless/component approach (Radix directly, no shadcn
  CLI)**: shadcn is a thin, well-maintained convention over exactly that
  (Radix + Tailwind + cva); hand-rolling the same scaffolding gains nothing.
- **`next-themes` for the toggle**: unnecessary dependency for a CSR-only
  app with no hydration mismatch to guard against.
