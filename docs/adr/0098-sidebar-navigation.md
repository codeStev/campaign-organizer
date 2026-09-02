# 0098. Sidebar navigation; Game Systems promoted to a top-level page

## Status
Accepted

## Context
Two related complaints. First: "Game systems" (ADR-0094) was deliberately
placed as a card embedded in the Templates page, reached from "exactly the
place a GM is already standing when they'd want to manage systems" — a
reasonable call when `GameSystem` was just `(id, name)`. ADR-0095 then grew
it into a real entity with a tagline, color, and notes, and gave it a direct
cross-context link from `Campaign.systemId` — so a system is no longer just
"the thing templates are grouped by," and burying its CRUD inside the
Templates page no longer matches what it is.

Second, separately: the app's top-level nav was two plain header links
(Templates, Settings), and each World's 13 tabs were a single crowded
horizontal row (`Tabs`/`TabsList` in `WorldView.tsx`). Both needed a real,
persistent menu rather than an ad hoc link list, and this is also the
natural moment to give Game Systems a proper top-level home — a new nav
entry only makes sense once there's a real nav to add it to.

## Decision
**Adopt the shadcn `sidebar` component** (`components/ui/sidebar.tsx`,
installed via `npx shadcn@latest add sidebar`, pulling in `sheet.tsx`,
`skeleton.tsx`, and `hooks/use-mobile.ts` as its dependencies) rather than
hand-rolling nav markup, per this project's shadcn-first convention. Two
independent sidebars, both `collapsible="none"` (always visible — this is a
small, single-user desktop-first tool; no need for the primitive's
offcanvas/icon-collapse/mobile-drawer modes):

- **`AppSidebar`** (`components/AppSidebar.tsx`): five flat peer entries —
  Worlds, Templates, Statblocks, **Game Systems**, Settings — rendered by a
  new `AppShell` wrapper in `App.tsx` for every top-level route.
  `TemplatesPageRoute`'s own local Templates/Statblocks nav and
  `SettingsPage`'s "← Worlds" button are dropped as redundant now that
  `AppSidebar` covers that navigation. Route paths for Templates/Statblocks
  stay nested (`/templates/global`, `/templates/statblocks`) to avoid
  churning bookmarks and the `globalTemplateId`/`globalStatblockId` deep-link
  params — only the sidebar treats them as flat peers, not the URL shape.
- **In-world sidebar** (`WorldView.tsx`): the same primitive, replacing the
  horizontal tab row, with the 13 tabs grouped under three
  `SidebarGroupLabel`s for scannability: **World** (Articles, Maps,
  Timelines, Calendars, Relationships, Tags), **Play** (Campaigns, Players,
  Sheets, Whiteboards, Tables & Decks, Handouts), **Tools** (Consistency).
  A World still takes over the screen with this sidebar instead of nesting
  under `AppSidebar` too — two sidebars at once would be clutter, not
  clarity.

**Game Systems CRUD moves to its own page**, `pages/GameSystemsPage.tsx` —
the exact state/handlers/JSX previously embedded in
`GlobalTemplatesPanel.tsx`, unchanged, just relocated behind a new
`/game-systems` route. `GlobalTemplatesPanel.tsx` keeps a read-only
`gameSystemsApi.list()` fetch for labeling templates and its
create-if-missing helper for builtin starters — it still needs to *use*
systems, just not manage them.

**New `--sidebar*` CSS custom properties** added to `index.css` (`:root`
and `.dark`), built from this app's existing brand-purple palette (ADR-0063)
rather than shadcn's generic Nova defaults, plus `--color-sidebar*` entries
in the `@theme inline` block so the component's `bg-sidebar`/
`text-sidebar-foreground`/etc. utilities resolve — the CLI doesn't patch a
non-standard `index.css` layout automatically, so these were added by hand.
Both sidebars share a `.sidebar-shell` class setting a fluid
`--sidebar-width: clamp(11rem, 18vw, 15rem)`, overridden to `100%` with
`flex-direction: column` at the existing 680px breakpoint (stacks above
content, matching how `.wiki-layout`/`.settings-layout` already collapse on
mobile elsewhere in this app).

Dead CSS removed as part of this: the old `.tabs`/`.tab`/`.tab.active`
rules and their mobile overrides, specific to the horizontal tab bar this
ADR replaces.

## Consequences
- `App.tsx`: outer `<main>` becomes a plain `<div className="app-body">` —
  the semantic `<main>` landmark now comes from `SidebarInset` inside
  whichever shell is active (`AppShell` or `WorldView`), avoiding nested
  `<main>` elements.
- Two nested nav levels now exist by design: `AppSidebar` (top-level) and,
  separately, `SettingsPage`'s own small AI-category sub-nav (kept, since
  Settings has room to grow more categories) — not a regression, an
  intentional two-level structure.
- FR-56/FR-57 (`docs/requirements.md`) describe what a game system *is*, not
  where its CRUD UI lives, so no requirements text changes.

## Alternatives considered
- **Dropdown/grouped header menu instead of a sidebar.** Rejected — the
  user explicitly asked for a persistent sidebar, and a 13-tab in-world nav
  reads better as a scannable grouped list than a dropdown.
- **Hand-rolled nav markup (plain `NavLink`s in a styled `<nav>`), as this
  ADR's first draft did before the shadcn `sidebar` component was pointed
  out.** Rejected in favor of the real primitive — this project's
  shadcn-first convention applies to sidebars as much as buttons or
  selects, and the primitive gets grouping, active-state, and (if ever
  needed later) collapse/mobile-drawer behavior for free.
- **Give Statblocks its own top-level route (`/statblocks`) to match Game
  Systems.** Rejected for now — only Game Systems was confirmed as needing
  a top-level *identity* change; Templates/Statblocks nesting is purely a
  URL detail the sidebar already papers over, not worth the deep-link churn.
