-- Optional session tagging for handouts (ADR-0077): lets the session packet
-- print pull in the handouts prepped for that night. A handout still exists
-- independently of any session; this is operational grouping for packet
-- assembly, not wiki-graph linking (ADR-0070's "props, not knowledge" stands).

ALTER TABLE handouts ADD COLUMN session_id UUID REFERENCES sessions(id) ON DELETE SET NULL;

CREATE INDEX idx_handouts_session ON handouts (session_id);
