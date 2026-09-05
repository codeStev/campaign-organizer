# 0107. Session calendar widget and persisted campaign color

## Status
Accepted

## Context
`Session.date` (`LocalDate`, nullable — real-world scheduling date, not the
in-fiction custom-calendar system `NextCalendarsView.tsx` implements) has
existed since the session model was introduced, but nothing renders it as
a calendar — only as raw text on the session's own page. The user wants a
month-grid calendar on both the world Overview page (every campaign's
sessions, each campaign visually distinct) and a campaign's own detail page
(just that campaign's sessions).

Telling campaigns apart visually requires a color per campaign. `Campaign`
has no color field today; only `GameSystem` does (`V43__game_system_details.sql`),
and multiple campaigns sharing a system (or none) would be indistinguishable
on a shared calendar. The user explicitly asked for this to be
user-assignable and persisted, not an automatic-only assignment.

## Decision

**Campaign color** — add `color VARCHAR(20)` to `campaigns`
(`V54__campaign_color.sql`), threaded through the domain/DTO/mapper/entity
stack identically to `GameSystem.color`: plain nullable string, no format
validation anywhere in the stack (frontend trusts a native
`<input type="color">`, same as `GameSystemsPage.tsx` already does). No new
abstraction — this is the same field shape as an existing, working pattern.

**World Overview data** — `WorldOverviewStats` (ADR-0102/0103) gains
`scheduledSessions: SessionCalendarEntry[]`, computed in the same
per-campaign fan-out `WorldOverviewService` already runs
(`CampaignQueryPort.findByWorld` → `SessionQueryPort.findOrdered` per
campaign): every session with a non-null `date`, carrying
`{sessionId, campaignId, campaignName, campaignColor, title, sessionNumber, date}`,
sorted by date. Uncapped, unlike `recentlyEdited`'s cap of 5 — total
session count per world stays small even over years of weekly play,
unlike an ever-growing article list.

**Campaign page** — no new endpoint; `NextCampaignsPage.tsx` already loads
the selected campaign's `sessions` into state, reused directly for its own
calendar widget.

**Fallback coloring** — a campaign with `color: null` (every campaign,
until the user picks one) still needs a legible, *consistent* color on the
calendar rather than a blank/invisible marker. `frontend/src/lib/campaignColor.ts`
resolves a display color for any campaign: its own `color` if set, else a
deterministic hash of `campaign.id` into a small fixed palette defined in
that file. This is a client-side-only concern — the fallback is never
persisted or computed server-side, and the same function is used on both
the world Overview and the campaign page so an uncolored campaign renders
identically everywhere. The existing `--chart-1..5` CSS tokens were
considered and rejected for this palette: they're deliberately low-chroma/
near-gray (this app's charts are intentionally muted), not distinguishable
enough for a calendar legend where the whole point is telling campaigns
apart at a glance.

**Calendar component** — `frontend/src/components/SessionCalendar.tsx`, a
plain month grid built on native `Date` math (no new dependency — none
exists in `frontend/package.json` today, and the format is simple enough
not to need one).

## Consequences
- `Campaign.color` behaves exactly like `GameSystem.color` — unvalidated,
  optional, purely a display concern. A malformed value (freeform text,
  not actually entered via the color input) would only affect that one
  campaign's own calendar dots, same blast radius as a bad `GameSystem.color`
  today.
- `WorldOverviewStats` keeps its "one composed response renders the whole
  screen" character (ADR-0103) — the calendar widget needs no separate
  round trip.
- `campaignColor.ts`'s fallback palette is a second color source alongside
  the persisted field; call sites must always go through
  `getCampaignColor()` rather than reading `campaign.color` directly, or an
  uncolored campaign will render inconsistently between screens.

## Alternatives considered
- **Server-computed fallback color** (persist a computed color at creation
  time, or return one from the API when `color` is null) — rejected: makes
  the "no color set" state ambiguous (was it deliberately picked, or
  defaulted?) and ties the fallback palette's definition to the backend,
  where an ADR-0107-only frontend concern doesn't need to live.
- **Reusing `--chart-1..5`** — rejected per the near-gray/low-chroma
  reasoning above; a calendar legend's entire job is visual distinction.
- **A new date library** (`date-fns`, `luxon`) for the month grid —
  rejected: native `Date` arithmetic for "days in this month, starting
  weekday" is a handful of lines, not worth a new dependency in a project
  that has none today.
