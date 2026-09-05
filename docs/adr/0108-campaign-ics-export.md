# 0108. Campaign .ics export

## Status
Accepted

## Context
The session calendar (ADR-0107) makes scheduled sessions visible inside the
app; the user also wants them in their own calendar app — both as a one-time
download and as a live subscription that auto-refreshes when sessions
change. `Session.date` is `LocalDate` with no time-of-day column at all
(`V10__campaigns.sql`), so any export maps sessions to all-day events, not
timed ones.

A live subscription needs a URL a calendar app can poll on its own schedule.
Calendar apps generally can't send this app's bearer token (ADR-0006's
stateless JWT auth), so the feed needs to authenticate a different way: an
unguessable secret embedded in the URL itself. This app already has exactly
this pattern — `GET /api/media/{id}/content` is `permitAll` in
`SecurityConfig`, addressed by an unguessable id, documented in ADR-0016 as
an accepted "security by obscurity" trade-off for a personal, single-user
app. The .ics feed follows the same precedent.

## Decision
New `interchange/calendar` subcontext (sibling to `interchange/packet`),
following the same published-port composition style
`SessionPacketService` already uses — no new dependency on `campaign`'s
persistence, only its already-published `CampaignQueryPort`/`SessionQueryPort`.

- **`CalendarFeed`** (`campaignId`, `token`, `createdAt`) — its own table,
  `campaign_calendar_feeds` (`V55__campaign_calendar_feed.sql`), keyed by
  `campaign_id` rather than a separate id (1:1 with the campaign). This is
  an **interchange-owned concern, not a `Campaign` field**: it's a
  subscription credential, not an attribute of what a campaign *is*, and
  keeping it in its own table means regenerating it (revoking a leaked URL)
  never touches the campaign aggregate, and the token never risks leaking
  into the general `CampaignResponse`/`CampaignView` returned by every
  campaign list/get call.
- **Token minting** uses this app's existing `IdGenerator` port (a random
  UUID), not a raw `UUID.randomUUID()` call in domain code — same
  determinism/testability convention every other aggregate's id already
  follows (ADR-0049 finding F3).
- **Three endpoints**, all under `/api/**`:
  - `GET .../calendar-feed` — get-or-create the token (lazy: minted on
    first use, not at campaign creation).
  - `POST .../calendar-feed/regenerate` — mint a fresh token, invalidating
    the old URL.
  - `GET .../calendar.ics` — authenticated one-time download.
  - `GET /api/calendar/{token}.ics` — the public feed, `security: []` in
    the contract (mirroring `/media/{mediaId}/content` exactly), and
    `permitAll` in `SecurityConfig` right beside that same media rule.
- **ICS generation** (`IcsCalendarBuilder`, package-private, hand-rolled —
  no new dependency for a format this simple): `VCALENDAR` wrapping one
  all-day `VEVENT` per dated session (`DTSTART`/`DTEND;VALUE=DATE`, `DTEND`
  = next day per RFC 5545's all-day convention), RFC 5545 TEXT escaping and
  75-octet line folding. Undated sessions are skipped — not calendar-relevant.
  An unknown token yields the same `NotFoundException` → 404 as any other
  missing resource, rather than a distinguishing error, so a bad token
  can't be used to probe for valid ones.

## Consequences
- Three round trips for one campaign's calendar UI (get campaign, get/create
  token to show the subscribe URL, download for the manual-export button) —
  acceptable; none of this is on a hot path, and each has an independent
  reason to be called separately (the token is shown once, downloads happen
  on demand).
- A leaked feed URL exposes that campaign's session titles/dates/summaries
  to whoever has it, same accepted trade-off ADR-0016 already made for
  media. Regeneration exists specifically so this is recoverable.
- `interchange/calendar` depends only on `campaign`'s published ports —
  deleting a campaign cascades its `campaign_calendar_feeds` row via the
  FK's `ON DELETE CASCADE`, no explicit cleanup needed in either context.

## Alternatives considered
- **Token as a `Campaign` field** — rejected: would expose (or require
  carefully excluding from) the secret on every existing campaign
  read/list/update path, and ties an export-specific credential's lifecycle
  to the campaign aggregate's own migrations/domain model for no benefit.
- **Reusing `campaign.id` as the token** (ADR-0016's literal pattern) —
  rejected: not revocable without changing the campaign's real id, and the
  user explicitly wants both a download and a regenerate-able subscription.
- **An external iCal library** (e.g. iCal4j) — rejected: the format needed
  here (a handful of all-day `VEVENT`s) is small enough to hand-roll
  correctly, not worth a new Gradle dependency.
- **Time-boxed/expiring tokens** — rejected as unnecessary complexity for a
  personal app; a manual "regenerate" action is a simpler, sufficient
  revocation mechanism than automatic expiry with no renewal flow.
