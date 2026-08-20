# ADR-0053: Deep linking via client-side routing

- Status: Accepted
- Date: 2026-08-20

## Context
The frontend has always been a single stateful tree: `App` holds `world`,
`WorldView` holds `tab`, and every list-detail view (`MapsView`,
`TimelinesView`, `CampaignsView`, `SheetsView` and its sub-panels, …) holds its
own `selected`/`draft` state as plain `useState`. None of it is reflected in
the URL — the app is always at `/`. Reloading, sharing a link, or using
browser back/forward loses your place; jumping to "the Statblocks tab" or "the
Goblin statblock" from outside the app is impossible.

## Decision
Adopt **`react-router-dom` v6** (`BrowserRouter` + `Routes`/`Route`, no
loaders/actions — the app doesn't need the v7 data-router APIs) and make the
URL the source of truth for **navigation location**: which world, which
top-level tab, which sub-tab (Sheets), and which single item is open within a
list-detail view.

**Route table:**
```
/worlds
/worlds/:worldId                                    → redirects to .../articles
/worlds/:worldId/articles[/:articleId]
/worlds/:worldId/maps[/:mapId]
/worlds/:worldId/timelines[/:timelineId]
/worlds/:worldId/calendars[/:calendarId]
/worlds/:worldId/relationships
/worlds/:worldId/campaigns[/:campaignId]
/worlds/:worldId/sheets                             → redirects to .../characters
/worlds/:worldId/sheets/characters[/:sheetId]
/worlds/:worldId/sheets/statblocks[/:statblockId]
/worlds/:worldId/sheets/templates[/:templateId]
/worlds/:worldId/whiteboards[/:whiteboardId]
```

**Scope boundary:** deep links reach one level into a list-detail view (the
open article/map/timeline/calendar/campaign/sheet/statblock/template/
whiteboard), matching how the sidebar navigation already works. They do
**not** reach inside a detail view's own interaction state — an expanded arc,
a beat being edited, a selected map pin, read/edit mode on an article,
ticked revision-diff pairs. Routing that deep would mean encoding many
independent, often multi-valued or transient UI states into the URL for
marginal benefit; those stay local `useState`, exactly as before.

**Pattern per view:** each list-detail component reads its id param via
`useParams()` and keeps a `useEffect` that loads/selects the matching item
whenever that id doesn't match what's currently loaded — this is what makes a
pasted URL, a page reload, and back/forward all work. Selecting an item (by
click, by "open article" from another tab, by creating one) calls
`navigate()` to the item's URL rather than only setting local state; the
effect above then does the actual load. Two resources (`timelines`,
`calendars`) have no `GET /.../{id}` endpoint on the backend — their effect
resolves the id against the already-loaded list instead of fetching directly,
since both views already load their full list up front.

**Auth interacts for free:** `App` still gates all routes behind `authed`
(unchanged `useState`), rendering `LoginPage` in place without touching the
URL. Because the URL never changes during the login gate, a deep link typed
before authenticating resolves correctly the moment `authed` flips true — no
"redirect back to where you were going" logic needed.

**Command palette:** the ADR-0034 palette's "Go to <tab>" commands and
article-jump commands now call `navigate()` instead of local `setTab`/
`openArticle`; behavior is otherwise unchanged.

## Consequences
- Every list-detail view gains a small, uniform "sync selection from the URL"
  effect — mechanical, but touches nine components (`WorldView`'s articles
  section, `MapsView`, `TimelinesView`, `CalendarsView`, `CampaignsView`,
  `CharacterSheetsPanel`, `StatblocksPanel`, `FieldTemplatesPanel`,
  `WhiteboardsView`).
- `nginx.conf` already has an SPA fallback (`try_files $uri $uri/ /index.html`)
  from ADR-0004's setup, so no deployment change is needed for direct loads of
  a deep path.
- Bookmarks, browser history, and pasted links now all work; sharing "look at
  this statblock" is a URL instead of a verbal set of directions.

## Alternatives considered
- **Hash-based routing** (`/#/worlds/...`): avoids any server config, but this
  app already has the SPA fallback in place, so there's no reason to take the
  uglier URLs.
- **URL search params instead of path segments** for the open item (e.g.
  `?statblock=<id>`): workable, but path segments read better as "a page" and
  match how the rest of the API is already shaped (`/worlds/{id}/statblocks/
  {id}`).
- **Encode detail-view interaction state too** (open pin, expanded arc, edit
  mode): rejected per the scope boundary above — large state-syncing surface
  for state that isn't really "a page" to link to.
