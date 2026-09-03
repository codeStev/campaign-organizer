# UI overhaul migration plan

Status: In progress — **Phases 1–5 done**, Phase 6 up next. Living
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
