-- Fantasy calendars with named months. See docs/adr/0020-fantasy-calendars-model.md.

CREATE TABLE calendars (
    id            UUID PRIMARY KEY,
    world_id      UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    name          VARCHAR(200) NOT NULL,
    days_per_week INTEGER,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_calendars_world_created ON calendars (world_id, created_at DESC);

CREATE TABLE calendar_months (
    id          UUID PRIMARY KEY,
    calendar_id UUID NOT NULL REFERENCES calendars(id) ON DELETE CASCADE,
    position    INTEGER NOT NULL,
    name        VARCHAR(100) NOT NULL,
    days        INTEGER NOT NULL
);

CREATE INDEX idx_calendar_months_cal ON calendar_months (calendar_id, position);

-- Timelines may reference a calendar for display and validation.
ALTER TABLE timelines ADD COLUMN calendar_id UUID REFERENCES calendars(id) ON DELETE SET NULL;
