# 0095. Game system details and campaign link

## Status
Accepted

## Context
ADR-0094 made `GameSystem` a real, top-level, world-independent entity,
but deliberately minimal — just a name. Brainstorming what belongs at
that layer (the user runs everything from crunchy systems like D&D 5e,
Pathfinder, and Pirate Borg to lightweight narrative ones like Vaesen and
Triangle Agency, so additions need to stay optional and skippable rather
than a form every system is forced to fill out) surfaced four candidates,
of which three are in scope here:

1. **Rule references** — the original stated motivation for making
   `GameSystem` a real entity in the first place.
2. **A short tagline and color badge** — cheap, useful once the catalog
   has several systems in it.
3. **`Campaign.systemId`** — nothing currently ties a *campaign* to a
   system directly; it's only ever inferable indirectly, via which
   templates its character sheets happen to use. The original framing
   for `GameSystem` was "a world can contain multiple campaigns which
   could differ in the game system" — that's a campaign-level fact.
4. **`Document.systemId`** — investigated and found to need **no
   changes**: `FieldTemplate.systemId` (world-scoped templates) already
   has no `kind`-based restriction in its validation, and the template
   editor's system picker (`TemplateBuilder.tsx`) already renders
   unconditionally regardless of `kind`. A `DOCUMENT`-kind template could
   already be tagged with a game system before this ADR. Recorded here so
   it isn't "rediscovered" as a gap later.

## Decision

### `GameSystem` gains `tagline`, `color`, `notes` — all optional
- `notes` is a single Markdown field (mirrors `Campaign.notes`/
  `Session.notes` elsewhere in this app), not a structured link list —
  flexible enough for "here's a link to the Vaesen SRD" and "we play
  Pathfinder with these three house rules" alike, without imposing
  structure that doesn't fit every system.
- `tagline` is a short one-line description; `color` is a free-text
  color value driving a small badge/swatch anywhere the system is
  referenced. Neither is validated beyond length — a system with no
  tagline or color set is exactly as valid as one with both.

### `Campaign.systemId` — nullable, cross-context
`Campaign` (in the `campaign` context) references `GameSystem` (in
`characters`) via the same ACL pattern this session has used repeatedly
(e.g. `campaign.application.session.port.out.CharacterSheetExistsPort`
wrapping `characters`' published `CharacterSheetQueryPort`): a new
`campaign.application.campaign.port.out.GameSystemExistsPort` (mirroring
`WorldExistsPort`'s single-method shape) implemented by an adapter
wrapping `characters`' already-existing `GameSystemQueryPort.existsById(...)`.

**Nullable, `ON DELETE SET NULL`** — a campaign can exist before its
system is decided, or run a genuinely system-agnostic oneshot; this
matches every other optional relationship already in this app
(`CharacterSheet.campaignId`, `Statblock.campaignId`, etc.). Deleting a
game system detaches any campaign referencing it rather than blocking
the delete or cascading — a campaign→system reference is informational,
unlike a template→system reference where the system is structurally load-
bearing for the template's identity (ADR-0093/0094's `RESTRICT` on
`global_field_templates.system_id` stays as-is; this is a different,
softer relationship).

### `Document.systemId` — no change, verified working
No decision to make here beyond confirming and recording the finding
above.

## Consequences
- Two new migrations: `V43__game_system_details.sql` (three nullable
  columns on `game_systems`), `V44__campaign_system.sql` (one nullable FK
  column on `campaigns`).
- `GameSystemImportPort.importOrReuse` (ADR-0094) keeps its existing
  "match by name, don't overwrite on reuse" behavior — an imported
  system's `tagline`/`color`/`notes` are only used when the system is
  genuinely new, not merged into an existing match.
- `ImportService`'s game-system resolution pass (previously built just
  before the field-template loops) moves earlier, before the campaigns
  loop, since campaigns now need to resolve their `systemId` through the
  same resolve-or-reuse map field templates already use.
- The global-templates management page's "Game systems" section (added
  in ADR-0094 as a bare name-only list) becomes an expand-in-place editor
  to fit the new fields, reusing the existing `MarkdownEditor` component.

## Alternatives considered
- **Structured `{title, url}` reference list instead of a Markdown notes
  field.** Rejected — more UI to build, less flexible for prose
  house-rules notes, and the simpler shape already matches this app's
  established `notes` convention elsewhere.
- **`World.systemId` instead of/alongside `Campaign.systemId`.**
  Rejected — a world already explicitly supports multiple systems across
  its campaigns (the whole reason `GameSystem` isn't world-scoped); a
  system belongs to the campaign, not the world.
