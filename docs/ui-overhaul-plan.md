# UI overhaul migration plan

Status: Draft, in progress. Living document — update it as phases complete
or decisions change, rather than letting it drift out of sync with reality.

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

## Open decisions to confirm before/during Phase 1

These came out of reviewing the mockup and directly affect Phase 1 — flag
and confirm rather than assuming:

- **Library merge.** The mockup merges Game Systems + Statblocks + Field
  Templates into one tabbed "Library" page. This is the *opposite* of
  ADR-0098, which just gave Game Systems its own top-level page. Pick one:
  keep them separate (current state) or merge under the new nav. Default
  assumption until told otherwise: **keep them separate** — ADR-0098 was a
  deliberate, recent, user-confirmed decision; the mockup wasn't built
  knowing that.
- **Nav grouping: World/Play/Library vs. current World/Play/Tools.** The
  mockup folds Consistency into "World" and has no separate "Tools" group;
  our current sidebar (ADR-0098) has a three-group World/Play/Tools split.
  Default assumption: adopt the mockup's two-group World/Play shape (drop
  "Tools", fold Consistency into World) for consistency with everything
  else adopted from it — revisit if it feels wrong once built.
- **Wiki's nested category tree.** The mockup shows a hierarchical
  category tree (Characters → Reckoners, Guild factors, …) which doesn't
  map to any existing data field — our closest concept is `ArticleTemplate`
  (flat: Character/Location/etc.) plus free tags. Needs its own small
  design pass (a tag-prefix convention like `Characters/Reckoners`, or a
  real parent-category field) before Phase 6 — not blocking earlier phases,
  since Wiki can ship with the current flat filter list first.

## Phases

Each phase is its own set of granular commits. Don't start a phase's
backend work without its ADR + requirements.md entry first (ground rule 4).

### Phase 1 — Shell + basic navigation
New `/next` shell: `AppSidebarNext` (World/Play/Library-or-Tools per the
decision above) and `WorldViewNext`'s in-world sidebar, both against
**stub/placeholder pages** (just a heading per screen, no real content
yet). Goal: the new nav itself is navigable and reviewable before any
feature work starts. Resolves the two nav-shape open decisions above as
part of doing this.

### Phase 2 — Reskin migrations (mostly-existing functionality)
Screens that are close to 1:1 with what already exists — port real content
into their Phase-1 stub, reusing existing API calls/hooks, no new backend:
- **Relations** (relationship graph — as-is)
- **Consistency report** (as-is)
- **Whiteboards** (as-is)
- **Chronicle** (merge Timelines + Calendars as sub-tabs of one screen —
  first real consolidation, but no new backend)
- **Wiki** (Articles, flat category list — ship without the nested tree;
  that's Phase 6)
- **Print Shop** (new aggregator screen: pick an output type, see a live
  paper preview, toggle options — but every underlying print output
  already exists, `PrintView`/`MapPrintView`/`StatblockCardsView`/handouts;
  this is a new front-end composition, not new print logic)

### Phase 3 — New backend features
Each gets its own ADR + FR + Flyway migration before any UI work:
- **Clocks** (progress/countdown trackers) — from the brainstormed backlog
  (memory `feature-backlog-brainstorm`), never built.
- **Loose Threads** (dangling plot hooks) — same backlog, never built.
- **Beat `kind`** (SCENE/COMBAT/REVEAL/…, each with a color) on the
  existing StoryArc/Beat domain — check current schema first; this may be
  additive (one new column) rather than a new concept.
- **Scratch/sandbox world flag** — a boolean or enum on `World`
  distinguishing real campaign worlds from a scratch/brainstorming world
  (the mockup's "Sketchbook · scratch" world switcher entry). Affects the
  Worlds list and World switcher UI.
- **World overview aggregate stats** — read endpoint(s) for article count,
  word count, sessions-run count, "recently edited" feed. Likely
  derivable from existing tables (revision history, article `updatedAt`)
  without new persisted state.

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
- **Wiki's nested category tree** — pick the tag-prefix-vs-real-field
  approach (see Open decisions), then build it.
- **Hierarchical Atlas** — one or more world/region maps linking down to
  more specific location sub-maps. The user explicitly flagged this as
  "a feature I want later," not blocking the rest of the overhaul.
- **Library merge** — if the open decision above ends up "yes, merge,"
  do it here (touches Game Systems/Statblocks/Templates nav placement,
  a second reversal of ADR-0098 territory, worth doing deliberately and
  separately).

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
