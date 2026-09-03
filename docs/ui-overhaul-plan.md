# UI overhaul migration plan

Status: In progress — **Phases 1–5, 5b, and 5c (mockup-fidelity polish) done**,
Phase 6 deferred by the user, so Phase 7 (Retirement) is next. Living
document — update it as phases complete or decisions change, rather than
letting it drift out of sync with reality.

## Context

The user commissioned a Claude Design mockup (`.dc.html`, source kept at
`/tmp/.../scratchpad/design-export/` for this session, not committed) to
get outside opinions on navigation and layout, since the app's current IA
(a header with two links, and 13 flat horizontal tabs per world) had grown
organically and the color palette was never a deliberate choice (ADR-0063's
purple was picked by an earlier session, not the user — see memory
`no-fixed-brand-color`). The mockup turned out to have several genuinely
better ideas — both structural (nav grouping, consolidated screens) and
functional (features never built, only brainstormed) — which this plan
turns into an actual, sequenced migration.

**What this plan is not**: a redesign of the visual style. That's already
done — ADR-0099 adopted shadcn's `mira` style + `stone` base color,
independent of everything below, specifically so color/spacing stays
swappable via the CLI rather than hand-picked again. This plan is about
**information architecture and features**.

## Ground rules (non-negotiable, per explicit user instruction)

1. **`master` always stays usable.** All of this happens on
   `feature/ui-overhaul` (already created, mira/stone style already
   committed there). Nothing here lands on `master` until it's ready, and
   even then in reviewable increments — not one giant merge.
2. **Build the new UI *next to* the old one, not in place.** New nav shell
   and new pages are new components/routes; existing pages keep working
   unchanged and reachable throughout, serving as the functional reference
   for what each rebuilt screen needs to cover. A screen's old
   implementation is only deleted after its replacement is verified to
   match or exceed it — never before.
3. **Tailwind + shadcn stays the architecture.** Do not adopt the mockup's
   own inline-styled approach. Use a real shadcn component wherever one
   applies — **including installing components we don't have yet**
   (Accordion, Progress, Badge, etc. as needed) — rather than hand-rolling
   markup, per the standing shadcn-first convention.
4. **Every new backend capability gets an ADR + a `docs/requirements.md`
   FR**, per this repo's standing workflow — scratch worlds and beat kinds
   are new domain concepts, not reskins. (Clocks and Loose Threads turned
   out to already exist, see Phase 3 note below — this rule still applied
   to them when they were originally built, ADR-0084/0085.)
5. **Playwright specs move with their screens.** A spec keeps testing the
   old route until that route is retired, then gets rewritten against the
   new one in the same change that retires the old screen — never left
   pointing at a deleted page.

## Mechanics: how "next to" actually works

New routes live under a `/next` prefix, mirroring the existing shape:
`/next/worlds`, `/next/worlds/:worldId/*`, with new top-level components
(`AppSidebarNext`, `WorldViewNext`, etc.) — not reusing `AppShell`/
`WorldView`'s JSX, though they do reuse the same shadcn primitives and API
client. A small link in both shells ("Try the new UI ↗" / "← Back to
current UI") lets the user jump between them to compare during the
transition. When a screen's `/next` version is confirmed to cover its old
counterpart, that old route/component is deleted and, if the whole nav
area is done, the `/next` prefix is dropped (routes promoted to the real
paths) — see Phase 7.

## Decisions (confirmed 2026-09-03)

Open questions from the mockup review, now resolved — recorded here for
context, not left as live questions:

- **Library merge → keep pages separate, but adopt the mockup's grouping
  philosophy.** Game Systems, Statblocks, and Field Templates stay three
  separate top-level pages (ADR-0098 stands) — but `AppSidebarNext` groups
  their three nav entries under a labeled **"Library · all worlds"**
  section, matching the mockup's framing that these are world-independent
  shared catalogs, distinct from World/Play. This is a nav-grouping change
  only, not new merged-page functionality — it belongs in **Phase 1**, not
  Phase 6.
- **Nav grouping → adopt the mockup's shape.** Two groups, **World** and
  **Play** — no separate "Tools" group; Consistency folds into World.
  Combined with the point above, `AppSidebarNext`'s in-world sidebar ends
  up World / Play, and the top-level `AppSidebarNext` ends up Worlds /
  Library (Game Systems, Statblocks, Templates) / Settings.
- **Wiki's nested tree → superseded, already built.** ~~Tag-prefix
  convention~~ was based on incomplete information — this app already has
  a real, unlimited-depth `parentArticleId` structural hierarchy
  (ADR-0080), independent of tags/categories, with a working
  collapsed-by-default sidebar tree in the old UI (`WorldView.tsx`'s
  `groupByParent`/`ArticleTreeItem`, now exported for reuse). Landed in
  Phase 2 alongside Wiki itself instead of waiting for Phase 6 — it was
  the same amount of work as the flat list once this was spotted, not
  worth shipping twice.
- **Beat `kind` → user-defined, not a fixed enum, but visually prominent
  like the mockup.** Same shape as Game Systems' color field: GMs create
  their own named kinds (not just SCENE/COMBAT/REVEAL) with a color, and
  beats render with that color the way the mockup shows — small CRUD, own
  table/entity, not a hardcoded enum. Bigger than originally scoped in
  Phase 3; still lands there, alongside its own ADR + FR.
- **Scratch/sandbox worlds → cosmetic only.** A flag/badge in the World
  list and switcher, nothing more — no functional exclusion from search,
  backups, or export. Simplest version, matches the mockup's "Sketchbook"
  entry exactly.
- **World Overview's "word count" stat → dropped.** Not worth building
  (on-read cost grows with world size; a cached/denormalized count is too
  much machinery for one stat). The Overview stats strip ships with
  article count, sessions run, loose threads, and flags only.

## Phases

Each phase is its own set of granular commits. Don't start a phase's
backend work without its ADR + requirements.md entry first (ground rule 4).

### Phase 1 — Shell + basic navigation ✅ done
New `/next` shell, per the decisions above:
- Top-level `AppSidebarNext`: Worlds, a **Library** section (Game Systems,
  Statblocks, Templates — three links, one label), Settings.
- In-world `WorldViewNext` sidebar: **World** (Articles/Wiki, Maps/Atlas,
  Chronicle, Relations, Consistency) and **Play** (Campaigns, Encounters,
  Whiteboards) — no separate Tools group.

Both against **stub/placeholder pages** (just a heading per screen, no
real content yet). Goal: the new nav itself is navigable and reviewable
before any feature work starts.

### Phase 2 — Reskin migrations (mostly-existing functionality) ✅ done
Screens that are close to 1:1 with what already exists — port real content
into their Phase-1 stub, reusing existing API calls/hooks, no new backend:
- **Relations** (relationship graph — as-is)
- **Consistency report** (as-is)
- **Whiteboards** (as-is)
- **Atlas** (maps — as-is; missing from the original draft of this list,
  added once spotted, same reuse-as-is treatment as the others)
- **Chronicle** (merge Timelines + Calendars as sub-tabs of one screen —
  first real consolidation, but no new backend)
- **Wiki** (Articles, with the real nested tree — see Decisions above;
  editing/creating stays on the old UI for now, read-only in `/next`)
- **Print Shop** (new launcher screen, bottom-pinned in the in-world nav
  like the mockup, not inside World/Play — missing from the original TABS
  list too, added alongside this). Scoped down from the mockup's inline
  live-preview + option toggles: "Full compendium" is a genuine reuse of
  the existing `PrintView` (opens its real paper view in a new window,
  `NewWindowPortal` — FR-30, "print in a separate tab" — confirmed
  end-to-end, not just that it renders); the other three outputs
  (session prep packet, statblock cards, player handouts) are scoped to
  screens not yet migrated, so they link out to the old UI for now. A true
  inline live-preview aggregator across all four is real net-new
  composition work, bigger than "reuse as-is" — left as an explicit
  follow-up, not silently implied by "Print Shop done."

**Mockup fidelity note (checked by rendering the actual mockup file and
comparing screenshots, not just reading its markup):** the shell — sidebar
proportions, full-viewport layout, Chronicle's tab pattern — matches the
mockup closely. Each reused screen's *internal* content layout does not:
Atlas/Whiteboards/Relations keep their existing list-then-canvas or
form-plus-list arrangement, where the mockup uses a full-bleed canvas with
a minimal click-to-select detail panel; Consistency shows grouped text
lists where the mockup has stat cards plus an actionable table. Expected,
since this phase's whole point is reuse-as-is rather than a rebuild — but
worth being explicit that "migrated" here means *relocated into the new
shell*, not *redesigned to match the mockup's own content layout*. Closer
per-screen fidelity, if wanted, is its own follow-up design pass, not
implied by this phase being done.

### Phase 3 — New backend features ✅ done

**Correction (2026-09-03):** the original draft of this phase listed
Clocks and Loose Threads as net-new work, sourced from the
`feature-backlog-brainstorm` memory (dated 2026-08-31). That memory was
stale — both are already fully built, backend and frontend: ADR-0084
(Clocks) and ADR-0085 (Loose Threads), full hex-arch slices under
`campaign/{domain,application,adapter}/{clock,loosethread}/...`,
`clocksApi`/`looseThreadsApi` in `frontend/src/api/client.ts`, and
existing pages `ClockBoard.tsx`/`LooseThreadsPanel.tsx`
(campaign/session-scoped, not world-scoped). No new backend work for
either — they move to **Phase 4** as a straight reuse/relocation
(surface the existing UI in `/next`), same treatment as Phase 2's
reskins. This is exactly the kind of drift the memory system's own
"verify before trusting" guidance exists for.

Genuinely new backend work, each gets its own ADR + FR + Flyway migration
before any UI work:
- ~~**Beat kinds**~~ ✅ done — ADR-0101, FR-61, world-scoped `BeatKind`
  catalog (name + color), `kindId` on `ArcBeat`. Kind picker + inline
  quick-add + colored dot wired into the old UI's `ArcBoard.tsx` (still
  the only place beats are created/edited — Campaigns/beats move to
  `/next` in Phase 5).
- ~~**Scratch/sandbox world flag**~~ ✅ done — ADR-0100, FR-60, boolean on
  `World`, cosmetic only (no functional exclusion from search/backup/
  export). Creation checkbox + badge on the old Worlds page and in the
  `/next` world switcher.
- ~~**World overview aggregate stats**~~ ✅ done — ADR-0102, FR-62,
  `GET /worlds/{worldId}/overview`: article count, sessions-run count
  (dated on or before today), 5 most recently updated articles. Pure
  read composition over existing published ports, no new persisted
  state, no UI yet — that's Phase 4's Overview dashboard.

### Phase 4 — Overview dashboard + Table Tools dock ✅ done
Consumes Phase 3's new data: world Overview screen (stats strip,
next-session card, recently-edited feed, Clocks widget, Loose Threads
widget), and the persistent **Table Tools dock** (dice roller +
roll-table shortcuts + mini Clocks view, toggleable from the top bar,
available from every `/next` screen). The dice roller and roll tables
already exist (FR-19, FR-40/41), and — per the Phase 3 correction above
— so do Clocks and Loose Threads; this phase turned out to be pure
composition/reuse, no new domain logic, matching Ground Rule 4's spirit.

Where the data itself needed more than Phase 3 initially shipped (next
session, open clocks, open loose threads — none of which had a
world-scoped published-port method to read from), that got filled in as
ADR-0103, not new UI-side logic — `NextOverviewPage.tsx` and
`TableToolsDock.tsx` are both thin consumers of `worldOverviewApi`.

**Dropped from the original bullet list: in-world date.** Nothing in
this app persists a campaign's or world's "current" in-world date —
`Calendar`/`Month` model calendar *structure*, not a live pointer into
it. Surfacing one would mean inventing new persisted state, which
contradicts this phase's whole "composition, not new domain logic"
premise (and Ground Rule 4 would require its own ADR/FR/migration first
regardless). Left as an explicit future FR if wanted, not silently
implied by "Phase 4 done."

### Phase 5 — Richer Campaigns workspace + Encounters ✅ done
- ~~**Campaigns**~~: merges Campaigns + Sessions + Beats (now with `kind`
  colors from Phase 3) + player roster/attendance + session todos + cheat
  sheet + print shortcuts + a new Chronicle link, into one workspace
  screen (`NextCampaignsPage.tsx`) — the mockup's biggest single
  consolidation. Structurally a near-copy of the old UI's `CampaignsView`:
  `SessionLog`/`ArcBoard`/`ClockBoard`/`RosterPanel`/`TodoListPanel` were
  already self-contained and dropped in unchanged. Cheat sheet and print
  shortcuts needed no separate work — `SessionLog` already provides both
  per-session.
- ~~**Encounters**~~: relocated the existing encounter builder (ADR-0097,
  `EncounterBoard`, unchanged) to its own `/next` nav entry
  (`NextEncountersPage.tsx`), with a campaign picker (encounters were only
  reachable via one campaign's detail view before) and a statblock
  reference panel beside the builder, mirroring `StatblocksPanel`'s
  list-plus-detail layout. No longer embedded inline in the Campaigns
  workspace — only the `encounters` list itself stays there, for
  ArcBoard's beat-linking picker.
- ~~**Collapsing usage-count badge**~~ applied to `ArcBoard`'s beat row:
  statblock/encounter chips collapse to "N statblocks"/"N encounters"
  badges (edit-mode chips, with remove buttons, are untouched). Shared by
  both UIs since `ArcBoard` is one component, not duplicated.
- ~~**Settings**~~ reskin (`NextSettingsPage.tsx`) — turned out to be
  smaller than the mockup implied: only "AI" (FR-39) is a real settings
  category today. Backup/import is a one-shot world action, not
  configurable state, and there's no user-facing access/password setting
  to reskin (ADR-0006: one env-configured password) — rather than
  fabricate sections with nothing to configure, Settings links to the
  worlds list (current UI) where backup/import already lives. Every
  `/next` route now renders real content — `NextStubPage` was dead code
  and got removed.

### Phase 5b — Remaining reskins (correction, 2026-09-03) ✅ done

**Gap found while checking Phase 7's precondition** ("every screen has a
confirmed-equivalent `/next` replacement"): the original phase list never
covered five of the old UI's 13 in-world tabs — **Tags, Players, Sheets,
Tables & Decks, Handouts**. None of Phases 1–5 mention them; they were
simply missed when the phase list was first drafted (same class of gap as
the earlier Atlas/Print-Shop omission and the Clocks/Loose-Threads
memory-drift, both caught and corrected mid-flight rather than shipped
wrong). Since Phase 7 (Retirement) explicitly can't start until every
screen has a `/next` equivalent, these need to land first. All five are
reused as-is (Phase 2's treatment), not rebuilt — each is already a
simple `{worldId, onOpenArticle?, onOpenStatblock?, onAuthExpired}`
self-contained component (`PlayersPanel`, `TagBrowseView`, `SheetsView`,
`TablesView`, `HandoutsView`), the same shape Whiteboards/Relations/
Consistency already were.
- ~~**Players**~~ — world-scoped player pool (FR-53), reused as-is.
- ~~**Tags**~~ — folksonomy cross-entity browse (FR-47), reused as-is.
- ~~**Sheets**~~ — character sheets/statblocks/documents/templates
  sub-tabs, reused as-is.
- ~~**Tables & Decks**~~ — roll tables + card decks (FR-19, FR-40/41),
  reused as-is.
- ~~**Handouts**~~ — player-facing printables (FR-46), reused as-is.

**Second gap found while wiring these in**: every one of these reused
views (plus Atlas and Whiteboards from Phase 2, plus Timelines/Calendars
from Chronicle) hardcoded absolute `/worlds/{worldId}/...` navigate()
calls, so clicking any list item under `/next` silently exited back to
the old UI — a regression that had been live since Phase 2 without being
caught by manual testing (clicking through to a *specific* list item was
never part of the verification checklist, only "does the screen render").
Fixed across all fourteen files by switching to React Router's
path-relative navigation (`{ relative: 'path' }` — the default "route"
mode doesn't do what it looks like it does for flat sibling routes;
verified against `@remix-run/router`'s own source, not assumed). Also
required replacing Chronicle's local-state tab switch with real nested
routes, since Timelines/Calendars read their selection from the URL
(ADR-0053) and a local tab had no URL segment for that to bind to.
**Lesson for future phases**: "reuse as-is" verification needs to include
clicking through to a specific list item, not just loading the list.

### Phase 5c — Mockup-fidelity polish pass
A full screen-by-screen comparison against the mockup, requested by the
user as "polish it to match the mockups layout and style (except colors)."
Explicitly out of scope: the current shadcn theme's actual color values
(kept as-is), and anything needing new backend data (see below). Landed,
one screen at a time:
- **Consistency**: rebuilt as a real Type/Where/Detail/Fix table instead of
  three separate bulleted lists; found and fixed a real bug along the way —
  `consistencyApi(worldId)` was called fresh every render, so the refresh
  `useCallback`'s dependency array never stabilized and the effect re-fired
  forever, hammering the report endpoint.
- **Settings**: single-column sectioned cards instead of a one-item side-nav;
  real "Export whole instance"/"Import backup" tiles wired to the same
  whole-instance `downloadBackup`/`importBackup` flow `WorldsPage.tsx` uses,
  replacing the old link-out to the current-UI worlds list.
- **Table Tools dock**: roll-table picker simplified from a Select+Button
  pair to a direct-click list.
- **Chronicle Timeline**: connecting line + dot markers down the left edge,
  date in a monospace column.
- **Chronicle Calendar**: months shown as a year-at-a-glance card grid
  instead of a definition list. **Deliberately not** a day-by-day event
  grid — no per-day event data exists in this model (a calendar only
  defines month names/lengths); building that would need new backend work.
- **Overview**: eyebrow (uppercase, letter-spaced) micro-labels on the three
  card headers.
- **Relations**: the create-relationship form collapses behind a
  `<details>` toggle so the sidebar defaults to the compact relationship
  list; the graph gets a full-bleed canvas area instead of a padded card.
- **Encounters**: combatant list rendered as a real Combatant/Qty table
  (shared `EncounterBoard.tsx`, so this also improves old UI's embedded
  encounter section).
- **Atlas**: pin legend/detail moved to a right-hand side column instead of
  stacking below the map, so the map canvas reads as the dominant element.
- **Campaigns workspace**: split into a session-tools main column
  (log/arcs/clocks) and a narrower roster/todos/GM-notes side column — a
  pure layout regrouping, no sub-component internals touched (avoided the
  much larger risk of rewriting `SessionLog`'s internals for a full
  mockup-exact 3-column rebuild).
- **Wiki**: tag chips added to the article read view (reuses the existing,
  already-`/next`-aware `TagList` component, plus an `articleTagsApi` fetch).
  Sidebar rebuilt as a real Category tree (ADR-0104), matching the mockup's
  "BY CATEGORY" grouping instead of ADR-0080's parentArticleId tree — a
  frontend-only change, since `Category` already had full backend CRUD sitting
  unused. Includes category create/delete, arbitrarily deep sub-categories,
  and drag-and-drop article-to-category assignment (`@dnd-kit/core`, this
  app's first drag-and-drop of any kind) — the first time in any UI there's
  a way to set an article's category at all. Also fixed a real navigation
  bug found along the way: `openArticle` used a bare relative `navigate(id)`,
  which broke once already on `wiki/:articleId` (same route-tree-aware
  relative-navigation issue fixed elsewhere in this app).
- **Whiteboards**: checked against the mockup — already matches (full-bleed
  corkboard canvas); no change needed.
- **Game Systems**: fixed a `/next` navigation gap — the sidebar's "Game
  Systems" entry linked to the old UI's `/game-systems` route, dropping the
  user out of `/next`'s chrome entirely (unlike Templates/Statblocks, which
  have the same gap, not yet fixed). Now mounted at `/next/game-systems`,
  reusing `GameSystemsPage` unchanged.

**Explicitly deferred, not attempted**: merging Game Systems/Statblocks/
Field Templates into one page (contradicts ADR-0098, which deliberately
keeps them as separate sidebar entries); Print Shop's live inline paper
preview (already deferred in Phase 2); fabricating Settings "Access"
password-change or "Snapshots" auto-backup UI (neither is a real backend
capability, and password handling is out of scope regardless); the dice
roller's per-die-button paradigm (would diverge from `DiceRollerWidget`,
reused everywhere in both UIs); Encounters' "Terrain & Stakes"/hazard-table
cards (new domain concepts, not in the current `Encounter` model — would
need an ADR + migration first, per this repo's Ground Rule 4); a true
day-by-day Calendar grid (see above).

### Phase 6 — Deferred / needs its own design pass
- **Hierarchical Atlas** — one or more world/region maps linking down to
  more specific location sub-maps. The user explicitly flagged this as
  "a feature I want later," not blocking the rest of the overhaul.

### Phase 7 — Retirement
Once every screen has a confirmed-equivalent `/next` replacement: delete
the old `AppShell`/`WorldView`/old page components and their routes,
rewrite the Playwright suite to target the (now-promoted) real paths
instead of `/next/*`, drop the `/next` prefix (promote routes), remove the
"try the new UI" escape hatch from both shells, update `CLAUDE.md` if the
bounded-context/page map changed materially.

## Verification per phase
- `cd frontend && npm run build` (TS + Vite) after every change.
- `npx playwright test` before closing out a phase — old suite must stay
  green throughout (it's testing the untouched old UI); add new specs for
  new `/next` screens as they land real content (Phase 2 onward).
- Manual click-through in a live `docker compose` stack for any phase that
  touches interaction/layout, not just a build check.
