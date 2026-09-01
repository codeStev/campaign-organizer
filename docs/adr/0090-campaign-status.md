# 0090. Campaign status

## Status
Accepted

## Context
Campaigns have no lifecycle state. Running lots of oneshots and multiple
concurrent campaigns means the campaign list accumulates everything ever
started, with no way to distinguish what's actually being run right now
from what's prepped-but-not-started or long dormant (issue #63... #64).

"Planned" matters as its own state, distinct from "active": a campaign
being prepped that hasn't run its first session yet. This also feeds the
future global landing page (#68) — a campaign stuck in "planned" or
"active" with nothing scheduled is itself a signal worth surfacing there.

ADR-0023 (the campaign manager's founding ADR) gave `Arc` a `status` enum
(`PLANNED, ACTIVE, COMPLETED, ABANDONED`) but deliberately scoped `Campaign`
to just name/description/notes — campaign-level status was punted, not
overlooked.

## Decision
Add a `status` field to `Campaign`, reusing the exact shape already
proven by `Arc.status` (domain enum, `@Enumerated(EnumType.STRING)` JPA
column, OpenAPI enum schema, MapStruct auto-mapping via matching
getter/field names) rather than inventing a new pattern.

- **Enum values:** `PLANNED, ACTIVE, ON_HIATUS, COMPLETED` — matches the
  issue's four states exactly. Deliberately not reusing `ArcStatus`
  (`ABANDONED` doesn't fit a campaign the way it fits a shelved arc, and
  "on hiatus" doesn't fit an arc) — a new `CampaignStatus` enum, not a
  shared one.
- **Default:** `PLANNED` when unset, mirroring `ArcStatus`'s default —
  a freshly created campaign is normally still being prepped, not running.
- **Threaded through the existing create/update path**, not a new
  use case or route: `Campaign` already has `UpdateCampaignUseCase` wired
  end-to-end (`PUT /worlds/{worldId}/campaigns/{campaignId}`), so `status`
  is simply a new field on `CreateCampaignCommand`/`UpdateCampaignCommand`,
  `Campaign` itself, `CampaignView`, the JPA entity, and the web
  request/response DTOs — additive, no new endpoints.
- **Frontend:** a status `Select` in `CampaignsView`'s header, next to the
  campaign name, calling the existing `campaignsApi().update()` (today
  only used for saving GM notes) with the new status — mirroring
  `ArcBoard`'s existing per-arc status selector.

## Consequences
- New Flyway migration `V36__campaign_status.sql` adds
  `status VARCHAR(20) NOT NULL DEFAULT 'PLANNED'` to `campaigns`, so every
  existing campaign in any deployed database becomes `PLANNED` on upgrade
  (a one-time, silently-wrong default for already-running campaigns —
  acceptable for a single-user local tool; the user can re-set status
  once after upgrading).
- `docs/api/openapi.yaml` gains a `CampaignStatus` schema and a required
  `status` field on `Campaign`/`CampaignRequest`, matching `ArcStatus`'s
  shape one-for-one.
- No new ADR-0023 amendment needed; this ADR is the record of the
  previously-punted decision being made.

## Alternatives considered
- **Reuse `ArcStatus` for campaigns too.** Rejected: the value sets
  genuinely differ (`ON_HIATUS` vs `ABANDONED`), and coupling two
  unrelated aggregates to one enum for six shared characters would be a
  false economy — see `docs/architecture/architecture-harness.md`'s
  emphasis on per-context ownership.
- **Derive status from session recency automatically** (e.g. "on hiatus"
  after N days with no session). Rejected as out of scope: the issue asks
  for an explicit, GM-set field, and automatic derivation is a
  meaningfully bigger feature (needs a scheduled job or read-time
  computation, plus a rule for what "recently" means) that can be a
  separate future issue if wanted.
