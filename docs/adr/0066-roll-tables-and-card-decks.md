# ADR-0066: Roll tables and card decks

- Status: Accepted
- Date: 2026-08-25

## Context
The GM wants reusable randomizers as first-class world content:

- **Roll tables**: any dice combination (`2d6`, `1d20+3`, `4d6kh3`), entries
  covering the possible results, free `[[wiki-links]]` to articles inside
  outcome text.
- **Card decks**: customized decks of titled cards (omens, complications),
  printable as cut-out cards.

They must be attachable to session prep so printing pulls them in, and —
the driving constraint — **an article prints exactly once per document**, no
matter how many sources reference it (beats, other articles, table entries,
deck cards), across every print surface (session packet, compendium,
standalone table/deck printouts).

Decisions taken with the owner up front: decks are **print-first with a
stateless draw** (no dealt/drawn bookkeeping); the attachment point is **the
beat** (alongside its article and statblock references); wiki-links inside
already-printed article bodies stay **anchors only** (they never pull their
targets into a printout).

## Decision
- **New bounded context `tables`** holding both aggregates — roll tables and
  card decks share one job (session randomizers) and nothing else wants half
  of it. Full hexagonal rings like every other context; registered in
  `ArchitectureTest.CONTEXTS`.
- **JSONB payload columns for entries/cards**, mirroring the whiteboard
  precedent (ADR-0027): an aggregate's rows are always loaded and saved
  whole, never queried individually, so separate child tables would only add
  migration and mapper surface. One table each (`roll_tables`, `card_decks`,
  V25) plus two beat join tables.
- **Domain validation lives in the aggregates**: entry ranges must sit
  inside the dice expression's result range, must not overlap, may be
  half-absent only as *one optional catch-all* entry ("else") covering the
  remaining results; gaps are allowed (a rolled gap just shows the total).
- **A pure domain `DiceExpression` parser** computes min/max. The grammar is
  deliberately duplicated from the `characters` dice roller (FR-19) rather
  than shared: cross-context imports of non-published classes are banned by
  the ArchUnit fitness function, and a shared-kernel module for one regex is
  not worth the coupling. The frontend mirrors the same parser again
  (`frontend/src/lib/dice.ts`) for live builder feedback; the server stays
  authoritative on save and on roll.
- **Beats gain `tableIds`/`deckIds`** (join tables, EAGER collections),
  validated through new campaign out-ports backed by the tables published
  ports — same shape as the statblock-exists flow.
- **The print-once rule lives solely in `SessionPacketService`.** Packet
  articles come from one `LinkedHashSet`: seeded by the beats' article ids,
  then extended with the articles resolved from every included table entry's
  and deck card's `[[wiki-links]]`. Resolution goes through a new
  `ArticleQueryPort.resolveRefs(worldId, names)` built on the same
  `ArticleRefIndex` the renderer uses — one source of truth for
  name→article lookup (title precedence over slug). First-seen order is
  preserved; packet consumers stay dumb.
- **Wiki-links in printed bodies are anchors only** — rendering reuses the
  normal article pipeline server-side (packet) or a client twin
  (`renderLinkedMarkdown`) in the compendium/standalone views; neither pulls
  linked articles into the document beyond what the dedup set already
  contains.
- **Usage/backlinks (FR-25) and backup/export (FR-36) learn the new types**:
  usage scans entry/card bodies for backlinks; export bundles both lists and
  import remaps their ids before beats persist their references.
- **Frontend**: a deep-linked "Tables & Decks" tab with builder UIs (dice
  input with live range readout, even-split helper, touch-friendly ↑/↓ card
  reorder — NFR-9), a cmdk-based `[[link]]` picker, a Roll button that calls
  the existing dice API and highlights the matching row, and a stateless
  Draw that highlights a random card. Printing surfaces: dedicated packet
  sections (table grid + cut-out deck cards), a compendium checkbox, and a
  standalone print window per table/deck (ADR-0038 pattern).

## Consequences
- Randomizers are world-scoped content like everything else: they show up in
  usage panels, round-trip through backups, and print without special cases.
- The dice grammar now exists in three places (backend roller, backend
  tables, frontend mirror). All three are small and tested; changing the
  grammar means touching all of them.
- The print-once guarantee is only as complete as the sources
  `SessionPacketService` knows about. A future referencing feature must
  extend that set — the single-site rule makes that an obvious, greppable
  change instead of a distributed one.
- Deck draws are not reproducible or trackable by design; if the owner ever
  wants dealt-card memory, that is a new decision, not a bug here.

## Alternatives considered
- **Separate `tables` and `decks` contexts**: more symmetric on paper, but
  each would be a thin half-context with identical plumbing; one context
  keeps the packet/export wiring in one place.
- **Normal child tables instead of JSONB**: needed only if rows were queried
  independently (they never are) or if referential integrity mattered inside
  the payload (it doesn't; ids are entry-local).
- **Attaching tables/decks to sessions directly**: beats are where prep
  material already gathers, and beats can move between sessions — attaching
  higher up would lose that reuse.
- **Server-side draw tracking** (which cards remain): contradicts the
  print-first decision; paper handles the discard pile better than software
  that needs a session to be open to work.
