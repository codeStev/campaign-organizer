-- Roll tables and card decks (FR-40, ADR-0066): world-scoped randomizer
-- content an article can be linked from and a beat can reference. Entries and
-- cards are stored as JSONB payloads; a table/deck is loaded and saved as a
-- whole (mirrors the whiteboards approach, ADR-0027).

CREATE TABLE roll_tables (
    id              UUID PRIMARY KEY,
    world_id        UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    title           VARCHAR(200) NOT NULL,
    description     TEXT,
    dice_expression VARCHAR(100) NOT NULL,
    min_result      INT NOT NULL,
    max_result      INT NOT NULL,
    entries         JSONB NOT NULL DEFAULT '[]',
    created_at      TIMESTAMPTZ NOT NULL,
    updated_at      TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_roll_tables_world ON roll_tables (world_id, created_at DESC);

CREATE TABLE card_decks (
    id          UUID PRIMARY KEY,
    world_id    UUID NOT NULL REFERENCES worlds(id) ON DELETE CASCADE,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    cards       JSONB NOT NULL DEFAULT '[]',
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_card_decks_world ON card_decks (world_id, created_at DESC);

-- Beats can reference roll tables and decks directly, mirroring beat_articles
-- and beat_statblocks; referenced tables/decks join the session packet.
CREATE TABLE beat_roll_tables (
    beat_id  UUID NOT NULL REFERENCES arc_beats(id) ON DELETE CASCADE,
    table_id UUID NOT NULL REFERENCES roll_tables(id) ON DELETE CASCADE
);
CREATE INDEX idx_beat_roll_tables_beat ON beat_roll_tables (beat_id);
CREATE INDEX idx_beat_roll_tables_table ON beat_roll_tables (table_id);

CREATE TABLE beat_card_decks (
    beat_id UUID NOT NULL REFERENCES arc_beats(id) ON DELETE CASCADE,
    deck_id UUID NOT NULL REFERENCES card_decks(id) ON DELETE CASCADE
);
CREATE INDEX idx_beat_card_decks_beat ON beat_card_decks (beat_id);
CREATE INDEX idx_beat_card_decks_deck ON beat_card_decks (deck_id);
