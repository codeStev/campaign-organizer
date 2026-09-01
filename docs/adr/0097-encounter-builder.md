# 97. Persisted encounter builder, linkable to arc beats

## Status
Accepted

## Context
ADR-0069 (FR-44) gave the app a print-only, disposable "encounter sheet":
pick statblocks, stage quantities and max HP, print — nothing saved. It
explicitly flagged this as a placeholder: *"if recurring encounters are
wanted later, that is a new aggregate with its own ADR."* Separately,
ADR-0043 (FR-33) let a beat reference statblocks directly, but its
"Alternatives considered" explicitly rejected "a dedicated encounter
entity" as "heavier than needed" at the time.

The user now wants exactly that: a persisted encounter — any number of
statblocks, with quantities, reusable and printable — that can be linked
to an arc beat. This is the trigger event both ADRs anticipated in
advance.

## Decision

### A new `Encounter` aggregate, campaign-scoped
Lives in a new `encounter` module inside the existing `campaign` bounded
context, following the same skeleton as `Clock`/`GameClock`
(`campaign/domain/clock/...`) — the cleanest existing campaign-scoped
aggregate with a list-valued child value object:

```java
Encounter(id, campaignId, name, notes, List<EncounterEntry> entries, createdAt, updatedAt)
EncounterEntry(statblockId, quantity)  // no own id
```

**No HP (or any other resource) override is persisted per entry.** An
earlier version of this ADR persisted a `maxHpOverride`, mirroring the
ad-hoc flow's staged max-HP field. That's wrong for this app's actual
system range: HP isn't universal (Forbidden Lands, Vaesen, and other
non-D&D-shaped systems don't track it, or track something else
entirely). Baking "HP" into persisted schema presumes a specific kind of
system. Instead, `EncounterEntry` only ever carries what's genuinely
system-agnostic — which statblock, how many — and whatever a combatant's
trackable resource is stays exactly where ADR-0069 already put it: live,
auto-detected from the statblock's own stats, and freely editable at
print time, for every entry, every time it's printed.

**PC-sheet participation stays print-time-only, not persisted.** Who's
actually at the table varies session to session even for a reused
encounter (the same "goblin ambush" run for different parties) — this
matches today's opt-in-checkbox behavior and needs no schema change.

### Entries get a real FK-backed join table, not JSONB
Unlike `Clock.segments` (purely descriptive, JSONB is the right fit),
`EncounterEntry.statblockId` is a genuine cross-context reference that
needs the same existence-check and referential-integrity treatment
`ArcBeat.statblockIds` already gets. `entries` is mapped as a JPA
`@ElementCollection` of an `@Embeddable` — same DB shape a plain id list
(`@CollectionTable`, real FK columns) would get, just with one extra
non-FK column (`quantity`) riding along on each row.

Validated the same way `ArcBeatCommandService.validateLinks` already
validates `statblockIds`: each entry's `statblockId` must exist in the
encounter's world (`StatblockExistsPort.existsInWorld`), checked on both
create and update. A fresh copy of that port lives in the new `encounter`
module rather than being shared with `arc`'s existing one — matching this
repo's established convention of one `*ExistsPort` per consuming module
(`arc`, `session`, and `tagging` each already keep their own).

### Beat ↔ encounter linkage extends ADR-0043's shape, doesn't replace it
`ArcBeat` gains a fifth id-list, `encounterIds: List<UUID>`, identical
treatment to `statblockIds`/`articleIds`/`tableIds`/`deckIds`: a new
`beat_encounters` join table (both FKs `ON DELETE CASCADE`), validated in
`ArcBeatCommandService.validateLinks`, editable via the same "+ link…"
chip-list pattern in `ArcBoard.tsx`.

**`ArcBeat.statblockIds` is untouched.** A beat can still reference a
loose statblock directly (a shopkeeper NPC doesn't need a full encounter
wrapped around it) — `Encounter` is additive, for the specific case of a
structured, quantified combat grouping.

Since `Encounter` and `ArcBeat` both live inside the `campaign` bounded
context, this link is validated via a direct intra-context dependency on
`Encounter`'s **published** `EncounterQueryPort.existsInCampaign(...)` —
no separate ACL adapter, the same pattern `GlobalFieldTemplateService`
already uses for its own sibling-module dependencies inside `characters`
(e.g. `StatblockTemplateRefPort`). A cross-context ACL adapter is only
needed where a genuine bounded-context boundary is crossed, e.g.
`EncounterStatblockExistsAdapter` delegating into `characters`.

### Interchange: ordinary import, not resolve-or-reuse
`Encounter` gets a normal `EncounterImportPort.importEncounter(view)` —
the resolve-or-reuse exception (ADR-0093/0094/0096) is reserved for
world-independent shared catalogs; `Encounter` is ordinary campaign data,
like `Clock` or `Session`, always minting a fresh id via the standard
`IdRemap` pass.

## Consequences
- New tables `encounters`, `encounter_entries`, `beat_encounters`
  (migration `V46`, with `V47` dropping the initially-added
  `max_hp_override` column once that design was reconsidered - see above);
  new REST route family `/worlds/{worldId}/campaigns/{campaignId}/encounters`;
  `Beat` request/response gain `encounterIds`.
- New frontend `EncounterBoard.tsx` (campaign view, mirrors `ClockBoard`),
  a small additive `initialEntries?: Record<string, {qty}>` prop on
  `EncounterSheetView.tsx` (quantity-only) so printing works from either a
  saved encounter or the original ad-hoc ticked-selection flow, and a
  fourth "+ link encounter" affordance in `ArcBoard.tsx`'s beat editor.
- The original ad-hoc "⚔ Encounter" flow from `StatblocksPanel.tsx`
  (ADR-0069) is untouched — it simply never passes `initialEntries`, so
  nothing about it persists, exactly as before.
- No FR currently covers persisted encounters; see FR-59. FR-33 and FR-44
  are extended by this ADR, not superseded.

## Alternatives considered
- **Nullable `beatId` on `Encounter`** (child points at its one parent
  beat), instead of the fifth id-list on `ArcBeat`. Rejected — breaks
  symmetry with the other four beat-link lists for no benefit, and
  forbids attaching one encounter to more than one beat with no
  compensating simplicity gain, since the list-based pattern was already
  proven working for statblocks/articles/tables/decks.
- **`entries` as a JSONB column**, mirroring `Clock.segments`. Rejected —
  loses the real FK integrity to `statblocks` that the existence-check
  validation and cascade-on-delete behavior depend on; `Clock.segments`
  has no cross-context reference to protect, `Encounter.entries` does.
- **Extend `ArcBeat.statblockIds` with an inline quantity** instead of a
  new aggregate. Rejected — quantities and reuse across beats/campaign
  printing need their own shape and lifecycle (create once, print many
  times, link to more than one beat); bolting that onto the existing
  plain id list would overload a field ADR-0043 deliberately kept simple.
