-- Session cheat sheets (FR-37, ADR-0071): one condensed, printable reference
-- per session. Fragments (freeform snippets, statblock refs, roll-table rows,
-- deck cards) are embedded as a JSONB payload and loaded/saved as a whole —
-- mirrors the whiteboards approach (ADR-0027).

CREATE TABLE cheat_sheets (
    id         UUID PRIMARY KEY,
    session_id UUID NOT NULL UNIQUE REFERENCES sessions(id) ON DELETE CASCADE,
    fragments  JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
