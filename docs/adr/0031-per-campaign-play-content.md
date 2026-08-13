# ADR-0031: Per-campaign play content (parties and beat detail)

- Status: Accepted (amends ADR-0023, ADR-0024)
- Date: 2026-08-13

## Context
Multiple campaigns already share one world's lore without duplication
(ADR-0023): sessions, arcs, and beats are per-campaign; everything else is
world-scoped. Two gaps remained for running several campaigns in one world:

1. Attaching a small, campaign-specific scene to a beat ("someone asks the party
   for help") required creating a **world article**, because the arc board UI only
   offered a title, an article link, and a done flag — even though the beat entity
   already has a free-text `body` and an optional `sessionId` (ADR-0023).
2. **Character sheets are world-level**, so two campaigns' parties share one list.

## Decision
- **Character sheets gain an optional `campaignId`** (nullable, FK to
  `campaigns`, `ON DELETE SET NULL`). Null = unassigned/shared. The sheet list
  accepts an optional `campaignId` filter, and the Sheets tab offers a campaign
  selector so each campaign's party is separate while templates, statblocks, and
  lore stay shared. Deleting a campaign unlinks its sheets (keeps them).
- **Beat notes and session links become first-class in the UI.** The arc board
  now edits a beat's `body` (its own detail) and links it to one of the
  campaign's sessions — using the existing `BeatRequest` fields, no model change.
  Campaign-specific scenes live on the beat; the article link stays optional for
  genuine world lore.

## Consequences
- Campaign scenes no longer pollute the shared wiki; the article link is reserved
  for real lore.
- Parties are separable per campaign without duplicating anything else; a sheet
  can still be left unassigned (shared) or reassigned.
- Only additive schema change (one nullable column); existing sheets read back
  with `campaignId=null`. World export (FR-22) carries the new field
  automatically.

## Alternatives considered
- **A separate "campaign notes/scenes" entity**: redundant — the beat `body` is
  exactly this; the fix is to expose it.
- **Campaign-scoping more entities (timelines, whiteboards)**: not needed now —
  timelines are already multiple-per-world (name one per campaign); revisit if a
  concrete need appears.
