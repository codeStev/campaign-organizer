-- Campaign and session todos (FR-54, ADR-0092): a lightweight GM task
-- list. A null session_id means a standing campaign-level todo; a
-- non-null one attaches it to that session.

CREATE TABLE todos (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    session_id UUID REFERENCES sessions(id) ON DELETE CASCADE,
    text TEXT NOT NULL,
    done BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_todos_campaign_id ON todos(campaign_id);
CREATE INDEX idx_todos_session_id ON todos(session_id);
