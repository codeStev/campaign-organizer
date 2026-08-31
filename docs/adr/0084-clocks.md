# ADR-0084: Clocks (segmented, labeled progress trackers)

- Status: Accepted
- Date: 2026-09-01
- Amends: ADR-0036 (session prep packet)

## Context
Systems like Blades in the Dark, Fabula Ultima, and Pirate Borg lean on
segmented "clocks" to track escalating threats or plans — a wheel of N
segments that fills up as pressure mounts. There is currently no way to
represent this. Per the issue: clocks are **per-campaign** scoped (a flat
list alongside Story Arcs, not per-arc or per-world), persist across
however many sessions it takes with no session-boundary reset, always fill
up from empty (a "countdown to doom" is just how the clock is named, not a
direction the app enforces), carry any GM-chosen segment count, and each
segment can *optionally* carry its own title/description — most segments
are just generic progress, only the narratively meaningful ones get
labeled. Clocks must also be **print-trackable**: a blank segmented diagram
in the session packet, marked by hand at the table (this app isn't open
mid-session, per ADR-0035), then updated in the app afterward.

## Decision
- **Clock is a sibling aggregate inside the existing `campaign` bounded
  context**, alongside `campaign`/`session`/`arc` — not a new bounded
  context. A clock is owned entirely by one campaign and read only by
  `campaign` itself and by `interchange` (session packet) through a
  published port, exactly the shape `arc` already has. This differs from
  tags (ADR-0083), which needed a new generic supporting context because
  they crossed two owning contexts; a clock has exactly one owner.
- **Segments are a JSONB array on the clock row**, not a child table —
  mirrors roll-table entries / card-deck cards (ADR-0066): a clock is
  always loaded and saved as a whole, segments are never queried
  independently, and there's no need for segment-level referential
  integrity.
- **Per-segment `filled: boolean`**, not a single `filledCount` int.
  Segments are individually meaningful (a labeled segment can represent a
  specific narrative beat), so each is independently addressable rather
  than only fillable in strict left-to-right order — the UI still nudges
  toward sequential fill (click a pip to fill up to it), but the domain
  doesn't enforce it.
- **Whole-object replace on update** — title, description, segments, and
  position all replace together on save, matching every other aggregate in
  this codebase (there are no PATCH endpoints anywhere). Resizing a clock
  is just sending a longer or shorter `segments` array.
- **No Cancel/draft UI** — clocks follow `ArcBoard`'s flat CRUD-list
  pattern (an inline edit persists immediately, with a toast confirming
  it), not the read/edit-split-with-Cancel pattern articles/statblocks use.
  Clicking a pip PUTs the updated segment list right away; there's no
  intermediate "unsaved" state to discard.
- **The packet's clock DTO is stateless — it carries no fill state at
  all.** `PacketClock` has `title`, `description`, and each segment's
  optional `title`/`description`, but no `filled` flag. The print view has
  nothing to blank out at render time because the data was never fill-aware
  to begin with — the same "stateless, print-first" principle FR-40 already
  states for card-deck draws (ADR-0066).
- **Campaign-scoped, unconditional packet inclusion.** `SessionPacketService`
  already includes a campaign's statblocks unconditionally (not filtered by
  beat reference); clocks get the same treatment — every packet for a
  campaign includes all of that campaign's clocks, since a clock has no
  beat linkage to filter by in the first place.
- **Dashboard synergy is out of scope here.** The issue asks for a future
  per-campaign dashboard (#67, not yet built) to surface the next unfilled
  segment's label as a prep hint. This ADR only ensures `ClockQueryPort` (the
  published port) exposes enough — campaign id, ordered segments with fill
  state and labels — for that feature to consume later without a data-model
  change.

## Consequences
- Adding clocks costs one new aggregate with the same shape as `arc`; no
  new bounded context, no new ArchUnit registration.
- A clock's segments have no independent identity beyond their position in
  the array — reordering isn't a first-class operation (not asked for), and
  removing a middle segment shifts every later segment's index. Acceptable
  because segments are edited by the owning GM directly in-app, not
  referenced from elsewhere.
- The print packet's clocks are permanently blank by construction — if a
  future feature wants the packet to show *current* fill state instead
  (e.g. an "as of last session" printout), that's a new DTO decision, not a
  bug here.

## Alternatives considered
- **A single `filledCount` int instead of per-segment booleans**: simpler,
  but loses the ability to mark a specific labeled segment out of order,
  which the issue's framing (segments as individually meaningful) implies
  should be possible.
- **Per-arc or per-world scope**: rejected per the issue's explicit
  requirement — clocks are campaign-wide trackers, independent of which
  arc is currently active.
- **A child table for segments** (proper rows with ids): unnecessary
  relational overhead for data that's always read/written as a whole,
  mirroring the same reasoning ADR-0066 already made for roll-table
  entries and deck cards.
