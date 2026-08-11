# ADR-0020: Fantasy calendars model

- Status: Accepted
- Date: 2026-08-11

## Context
Timelines (ADR-0019) store integer `year/month/day` and are calendar-agnostic.
FR-10 adds custom calendars so fictional dates read as "3rd of Frostfall, 1000"
and so month/day values can be validated.

## Decision
- **Calendar = name + ordered months.** A `calendar` has a list of months, each
  with a `name` and a number of `days`, stored in a child `calendar_months`
  table ordered by `position` (not JSONB — avoids ORM/JSON-serialization risk and
  keeps months queryable). An optional `daysPerWeek` is recorded for display.
- **Full-replace months.** Create/update sends the complete months array; the
  server replaces the child rows in one transaction. Simple and race-free for a
  small list.
- **Timelines may reference a calendar** (`timelines.calendar_id`, nullable, set
  null on calendar delete). This is display/validation metadata only; event
  storage stays numeric per ADR-0019.
- **Event validation against the calendar.** When an event's timeline has a
  calendar, `month` must be within the month count and `day` within that month's
  length; otherwise `400`. Without a calendar, months/days are free integers.
- Calendars are world-scoped; deletes cascade to their months.

## Consequences
- Dates render with real month names; impossible dates ("day 40 of a 30-day
  month") are rejected once a calendar is attached.
- Existing calendar-less timelines keep working unchanged.
- Editing a month's length does not rewrite historical events; only future
  validation uses the current definition (acceptable — events store raw numbers).

## Alternatives considered
- **Months as JSONB on the calendar**: fewer tables, but adds Hibernate/Jackson
  JSON-mapping surface we'd rather not depend on under Spring Boot 4.
- **Weekdays, leap rules, moons**: out of scope now; the model can grow.
