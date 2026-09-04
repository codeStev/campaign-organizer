# 0106. `/next` gets its own component for every screen, no longer shares with old UI

## Status
Accepted

## Context

Phase 5b of `docs/ui-overhaul-plan.md` (`feature/ui-overhaul`) deliberately
reused nine old-UI page components as-is in `/next` — `SheetsView` (plus its
four sub-panels `CharacterSheetsPanel`/`StatblocksPanel`/`DocumentsPanel`/
`FieldTemplatesPanel`), `TablesView`, `HandoutsView`, `PlayersPanel`,
`TagBrowseView`, `RelationshipsView`, `WhiteboardsView`, `ConsistencyView`,
and `MapsView` — mounting the exact same component instance from both
`WorldView.tsx` (old UI) and `WorldViewNext.tsx` (`/next`). This was an
explicit time-saving shortcut at the time: each of these screens was already
self-contained (`{worldId, onAuthExpired, ...}` props, no dependency on
either shell), so reuse cost nothing structurally and let Phase 5b close out
quickly.

The user reversed that decision mid-session: *"i do not want the views
between old and new to be shared. The old views do NOT match the look and
feel of the new design."* The trigger was a concrete case — `/next`'s Sheets
screen needed to stop showing a `DiceRollerWidget` (redundant with the new
Table Tools dock, ADR-0105's slide-in panel), which meant either prop-gating
the shared component (`showDiceRoller?: boolean`, threaded through only for
`/next`'s benefit) or giving `/next` its own copy. The user rejected the
prop-gating path: sharing one instance means every future `/next`-only
adjustment either has to leak into old UI's file as an optional prop or gets
skipped, and old UI is scheduled for deletion in Phase 7 regardless — there
is no long-term value in keeping the two in lockstep.

## Decision

Every screen `/next` previously mounted directly from an old-UI page
component now has its own `Next`-prefixed fork instead:

| Old UI component | `/next` fork |
|---|---|
| `SheetsView` | `NextSheetsPage` |
| `CharacterSheetsPanel` | `NextCharacterSheetsPanel` |
| `StatblocksPanel` | `NextStatblocksPanel` |
| `DocumentsPanel` | `NextDocumentsPanel` |
| `FieldTemplatesPanel` | `NextFieldTemplatesPanel` |
| `PlayersPanel` | `NextPlayersPanel` |
| `TagBrowseView` | `NextTagBrowseView` |
| `RelationshipsView` | `NextRelationshipsView` |
| `WhiteboardsView` | `NextWhiteboardsView` |
| `ConsistencyView` | `NextConsistencyView` |
| `HandoutsView` | `NextHandoutsView` |
| `MapsView` | `NextMapsView` |
| `TablesView` | `NextTablesView` |

Each fork started as a straight duplicate-and-rename (component name only —
no `Next` in the exported prop interface name, since props were already
generic), then diverged where `/next` genuinely needed to (so far, only
`NextSheetsPage`: no `DiceRollerWidget` at all, vs. old UI's `SheetsView`
which keeps it since old UI has no Table Tools dock). Old UI's originals are
byte-for-byte unchanged — this ADR touches zero files under old UI's own
render path (`WorldView.tsx` and everything it imports).

This is **not** a visual redesign. This codebase's `.card`/`.editor-actions`/
`.wiki-layout` CSS vocabulary and shadcn primitives (ADR-0099) are already
the shared design language across both UIs — `NextCampaignsPage.tsx`, a
screen built `/next`-native from the start back in Phase 5, uses the exact
same classes as these newly-forked screens. The point of this ADR is
decoupling the **component instance**, not introducing a second visual
language. A from-scratch redesign of any individual screen remains available
as separate, future work if the user asks for one.

## Consequences

- `/next` and old UI now share zero page-level components (verified: no
  overlapping `./X` import between `WorldView.tsx` and `WorldViewNext.tsx`).
  Cross-cutting leaf widgets used by both — `TagInput`, `MarkdownEditor`,
  `CategoryTree`, `DiceRollerWidget`, `ConfirmDeleteDialog`, `PromptDialog`,
  and Campaigns' `SessionLog`/`ArcBoard`/`ClockBoard`/`RosterPanel`/
  `TodoListPanel` — are unaffected; those are building blocks, not full
  screens, and were never the target of the user's objection.
- `/next` can now diverge freely per screen (as it already has for Sheets'
  dice roller) without touching old UI, and vice versa — old UI stays frozen
  as the untouched reference until Phase 7 deletes it.
- This roughly doubles the line count of the nine affected screens in the
  repo (~4600 lines duplicated across 13 new files) until Phase 7 removes
  the old-UI half. Accepted as the cost of decoupling; not deduplicated via
  a shared base component, since the two copies are expected to diverge
  over time, not stay identical.
- Every one of Phase 5b's original screens keeps working exactly as before
  from a user perspective — this is a pure internal restructuring, verified
  screen-by-screen via `npm run build`, the full Playwright suite (unchanged,
  17/17 passing — none of the existing specs target `/next` routes for these
  screens), and a live click-through of each fork in the docker-compose
  stack.
- `docs/ui-overhaul-plan.md`'s Phase 5b section is not rewritten (it
  correctly records what was decided and why at the time) but a new Phase
  5e entry cross-references this ADR as the reversal.

## Alternatives considered

- **Prop-gate the shared components** (`showDiceRoller?: boolean` and
  similar per future divergence) — the immediate trigger for this decision;
  rejected once it was clear this pattern would recur for every future
  `/next`-only tweak, each one leaking an optional prop into old UI's file.
- **Extract shared base logic, two thin presentational wrappers** — not
  attempted: most of these screens braid data-fetching, form state, and
  rendering together (matching this app's established per-screen pattern
  elsewhere), so a clean logic/presentation split would be a much larger
  refactor than the user's request called for. A straight fork ships the
  same outcome (independent files) at a fraction of the effort, and nothing
  prevents extracting shared logic later if the two copies stay similar
  enough to warrant it.
- **Do the full sweep in one pass vs. one screen at a time** — the user was
  asked directly (`AskUserQuestion`) and picked screen-by-screen, starting
  with Sheets; all nine were completed across the same session regardless,
  each as its own commit.
