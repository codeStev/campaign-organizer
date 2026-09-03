# UI overhaul migration plan

Status: In progress — **Phase 1 done**, Phase 2 up next. Living document —
update it as phases complete or decisions change, rather than letting it
drift out of sync with reality.

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
   FR**, per this repo's standing workflow — Clocks, Loose Threads, scratch
   worlds, and beat types are new domain concepts, not reskins.
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
paths) — see Phase 6.

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
- **Wiki's nested category tree → tag-prefix convention, arbitrary depth.**
  A tag like `Characters/Reckoners/Retired` parses into a tree at render
  time on its `/`-delimited segments — not capped at one level of nesting,
  so the tree can go as deep as the user's tags do. No new backend field;
  built entirely from existing free-tag data. Still a Phase 6 build (needs
  the parser + tree-rendering component), but the approach itself is
  locked in, not open.
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

### Phase 2 — Reskin migrations (mostly-existing functionality)
Screens that are close to 1:1 with what already exists — port real content
into their Phase-1 stub, reusing existing API calls/hooks, no new backend:
- **Relations** (relationship graph — as-is)
- **Consistency report** (as-is)
- **Whiteboards** (as-is)
- **Atlas** (maps — as-is; missing from the original draft of this list,
  added once spotted, same reuse-as-is treatment as the others)
- **Chronicle** (merge Timelines + Calendars as sub-tabs of one screen —
  first real consolidation, but no new backend)
- **Wiki** (Articles, flat category list — ship without the nested tree;
  that's Phase 6)
- **Print Shop** (new aggregator screen: pick an output type, see a live
  paper preview, toggle options — but every underlying print output
  already exists, `PrintView`/`MapPrintView`/`StatblockCardsView`/handouts;
  this is a new front-end composition, not new print logic)

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

### Phase 3 — New backend features
Each gets its own ADR + FR + Flyway migration before any UI work:
- **Clocks** (progress/countdown trackers) — from the brainstormed backlog
  (memory `feature-backlog-brainstorm`), never built.
- **Loose Threads** (dangling plot hooks) — same backlog, never built.
- **Beat kinds** — a small user-managed catalog (name + color, same shape
  as `GameSystem`'s color field), referenced by a new `kindId` on the
  existing StoryArc/Beat domain. Not a fixed enum — own table, own CRUD.
- **Scratch/sandbox world flag** — a boolean on `World`, cosmetic only
  (no functional exclusion from search/backup/export). Affects the Worlds
  list and World switcher UI only.
- **World overview aggregate stats** — read endpoint(s) for article count,
  sessions-run count, "recently edited" feed. No word count (dropped —
  see Decisions). Likely derivable from existing tables (revision history,
  article `updatedAt`) without new persisted state.

### Phase 4 — Overview dashboard + Table Tools dock
Consumes Phase 3's new data: world Overview screen (stats strip,
next-session card, recently-edited feed, Clocks widget, Loose Threads
widget, in-world date), and the persistent **Table Tools dock** (dice
roller + roll-table shortcuts + mini Clocks view, toggleable from the top
bar, available from every `/next` screen). The dice roller and roll
tables already exist (FR-19, FR-40/41) — this is composition, not new
roll logic.

### Phase 5 — Richer Campaigns workspace + Encounters
- **Campaigns**: merge Campaigns + Sessions + Beats (now with `kind`
  colors from Phase 3) + player roster/attendance + session todo + cheat
  sheet + print shortcuts + a Chronicle link, into one workspace screen —
  the mockup's biggest single consolidation.
- **Encounters**: relocate the existing encounter builder (ADR-0097) into
  its own `/next` nav entry with the statblock side panel, matching the
  mockup — mostly reskin/relocation, encounter builder logic is recent
  and already solid.
- Also apply the **collapsing usage-count badge** convention here and
  wherever else a screen currently lists individual reference chips for a
  count (e.g. "3 beats staged" instead of three separate chips) — a
  display-only pattern, apply during each screen's migration rather than
  as a separate pass.
- **Settings** reskin (existing AI/backup/access sections, visually
  consolidated to match the mockup).

### Phase 6 — Deferred / needs its own design pass
- **Wiki's nested category tree** — build the `/`-delimited tag-prefix
  parser + tree component (see Decisions above; approach is locked in,
  this is just the build).
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
