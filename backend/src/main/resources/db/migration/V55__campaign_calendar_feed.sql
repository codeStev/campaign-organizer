-- Campaign calendar feed (ADR-0108): a per-campaign secret token
-- addressing a public, unauthenticated .ics subscription endpoint —
-- interchange-owned, not a Campaign field, so it can be regenerated
-- (revoking the old URL) without touching the campaign aggregate itself.

CREATE TABLE campaign_calendar_feeds (
    campaign_id UUID PRIMARY KEY REFERENCES campaigns(id) ON DELETE CASCADE,
    token UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL
);
