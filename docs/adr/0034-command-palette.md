# ADR-0034: Command palette for in-world navigation

- Status: Accepted
- Date: 2026-08-14

## Context
A world exposes eight top-level tabs (Articles, Maps, Timelines, Calendars,
Relationships, Campaigns, Sheets, Whiteboards) plus nested arc/session/sheet
views. Reaching a known article means picking the right tab, then scrolling or
searching, then clicking. There was no global "jump to anything" affordance and
no keyboard shortcuts anywhere in the UI.

## Decision
Add a client-only **command palette** opened with `Ctrl/⌘-K` (and a "⌘K Jump…"
button in the world bar). It lists two command groups:

- **Navigate** — one entry per tab (`Go to <Tab>`), switching the active tab.
- **Articles** — one entry per article, which opens it in the Articles tab
  (reusing the existing `openFromMap` path).

The palette (`components/CommandPalette.tsx`) is a generic, presentational
component: it takes a `Command[]` (`{ id, label, group, keywords?, run }`) and
handles substring filtering (over label + keywords), keyboard navigation
(↑/↓/Enter/Esc), and overlay dismissal. `WorldView` builds the command list.

The palette fetches the **full, unfiltered** article list on open (via the
existing `GET /articles`), independent of the sidebar's search/campaign filter,
so any article is always reachable. Results are capped at 50 rows.

No backend, contract, or schema changes: this is composed entirely from existing
endpoints and state.

## Consequences
- Navigation to a known article drops from tab→scroll→click to `Ctrl-K`, type,
  Enter.
- The `Command` shape is extensible: maps, campaigns, sheets, or actions
  ("New article") can be added as more command sources later without touching
  the palette component.
- Opening the palette costs one `GET /articles`; acceptable for a single-user
  app and always current.
- Article titles are the only searchable text for now; full-body search stays in
  the sidebar's existing trigram search.

## Alternatives considered
- **Reusing the sidebar search instead of a palette** — rejected: the sidebar is
  Articles-only and filtered; it can't switch tabs and isn't reachable by
  keyboard from other tabs.
- **Server-side palette search endpoint** — unnecessary at single-user scale;
  the article list is small enough to filter client-side.
