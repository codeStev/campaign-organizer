-- Session transcripts (ADR-0121): one per session, replaced as a whole by the companion export.
-- Lines are one JSONB payload (like cheat_sheets.fragments): only in-character and narration
-- lines, never a player name or id. Tier 3 RLS through the parent session; deleting the session
-- deletes its transcript.

CREATE TABLE session_transcripts (
    id          UUID PRIMARY KEY,
    session_id  UUID NOT NULL UNIQUE REFERENCES sessions(id) ON DELETE CASCADE,
    lines       JSONB NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

ALTER TABLE session_transcripts ENABLE ROW LEVEL SECURITY;
ALTER TABLE session_transcripts FORCE ROW LEVEL SECURITY;
CREATE POLICY session_transcripts_owner_isolation ON session_transcripts
  USING (session_id IN (SELECT id FROM sessions));
