-- Loose threads (FR-49, ADR-0085): retrospective, session-scoped notes on
-- improvised details players latched onto. A denormalized campaign_id lets
-- a future dashboard query "open threads for campaign X" without walking
-- every session in the campaign.

CREATE TABLE loose_threads (
    id          UUID PRIMARY KEY,
    session_id  UUID NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    text        TEXT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_loose_threads_session ON loose_threads (session_id);
CREATE INDEX idx_loose_threads_campaign_status ON loose_threads (campaign_id, status);
