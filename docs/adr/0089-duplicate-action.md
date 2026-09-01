# ADR-0089: Duplicate action for statblocks, handouts, tables/decks, templates

- Status: Accepted
- Date: 2026-09-01

## Context
Reskinning a monster, reusing a handout shell, or starting a new oneshot
from an existing roll table/card deck currently means rebuilding it
field-by-field — especially painful running lots of oneshots across
systems. Per the issue: a straightforward "Duplicate" (copy-then-rename)
action on statblocks, handouts, roll tables/card decks, and field
templates. No merge/diff UI needed.

## Decision
- **Every duplicate routes through the aggregate's existing `create(...)`
  use case**, not new persistence/domain logic. A new `Duplicate<X>UseCase`
  method loads the source, builds the same `Create<X>Command` the normal
  create flow already takes (copying every field from the source, renaming
  the primary name/title field with a `" (copy)"` suffix), and delegates to
  `create(...)`. This is not just less code — it's *correct by
  construction* for the two aggregates with nested collections:
  `RollTableEntry`/`DeckCard` each carry their own id, but
  `CreateRollTableCommand`/`CreateCardDeckCommand` take id-less
  `EntryInput`/`CardInput` lists, and the existing service always mints a
  fresh id per entry/card on create. Mapping loaded entries/cards down to
  their `*Input` shape (dropping the old id) and going through `create`
  gives every duplicated entry/card a correct fresh id for free, with zero
  new id-minting logic to get wrong.
- **Chaining references copy through unchanged.** `nestedTableIds`/
  `nestedDeckIds` (FR-41/ADR-0072) point at *other* tables/decks, never at
  anything local to the table/deck being copied, so they're valid on the
  duplicate exactly as they were on the source — no remapping needed.
- **Straightforward field-for-field copy, no smart per-type defaults.** A
  duplicated Handout keeps its `sessionId` and `revealed` flag exactly as
  the source had them. The issue asks for "straightforward copy-then-
  rename," not type-specific judgment calls about what should reset —
  applying that literally, uniformly, across all five is itself the
  simplicity being asked for. (If experience later shows this is the wrong
  default for a specific field, that's a new, deliberate decision — not an
  oversight here.)
- **`FieldTemplate` duplicates carry over the same `kind`** — immutable
  after creation (ADR-0052), and `CreateFieldTemplateCommand` already
  accepts `kind`, so this falls straight out of "copy every field," no
  special case required.
- **No migration.** None of the five tables has a uniqueness constraint on
  its name/title column, so `" (copy)"` renaming can never collide with a
  database constraint.
- **UI: one "Duplicate" action per aggregate, in its detail/edit panel next
  to Delete** — the one location already present and consistent across all
  five, even though list-row markup varies a lot between them (some panels
  have no row-level actions at all today). After duplicating, the UI opens
  the new copy, matching how each panel already behaves right after a
  normal create.

## Consequences
- Five near-identical, small additions (one in-port, one service method,
  one controller route, one frontend client method, one UI button) rather
  than one shared "duplicate" mechanism — consistent with this codebase's
  existing pattern of independent, parallel aggregates with no shared
  instance-CRUD base class (confirmed again by ADR-0088's exploration of
  the same question for a different feature).
- A duplicate is indistinguishable from a manually-recreated copy once
  created — no "duplicated from X" provenance is tracked. Nothing in the
  issue asks for that, and it would be new bookkeeping for no stated need.
- Because duplication reuses `create`'s existing validation (world/campaign/
  template-kind checks, nested-reference checks), a duplicate can never
  produce an aggregate the normal create flow couldn't have produced
  directly — no new invariants to maintain.

## Alternatives considered
- **A generic, reusable "duplicate" mechanism** (e.g. a shared backend
  utility or a client-side get-then-repost helper used by all five):
  rejected — the five aggregates have different field shapes (some with
  nested id-bearing collections, one with an enum needing string
  conversion, one with an immutable discriminator), so a generic mechanism
  would need per-type branching anyway; five small, obvious, independently
  readable implementations are simpler to verify than one clever generic
  one.
- **Client-side duplicate** (fetch the full record, transform it into a
  create-request body, POST): would need zero backend changes, but
  leaves "how to construct a valid duplicate" logic and validation
  duplicated in the frontend instead of behind the same use-case port that
  already owns it for every other mutation in this codebase — inconsistent
  with how every other write in this app works.
- **Resetting type-specific fields on duplicate** (e.g. un-revealing a
  duplicated handout, clearing its session tag): more "thoughtful," but
  the issue's own framing ("straightforward copy-then-rename") argues
  against inventing per-type judgment calls that weren't asked for.
