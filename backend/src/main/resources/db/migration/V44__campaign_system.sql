-- Campaign game-system link (FR-57, ADR-0095): a campaign can optionally
-- declare which game system it runs, instead of that only being
-- inferable indirectly via which templates its character sheets use.
-- Nullable and ON DELETE SET NULL: a campaign can exist before its
-- system is decided, or run a system-agnostic oneshot; this is an
-- informational reference, not structurally load-bearing like a
-- template's system_id.

ALTER TABLE campaigns
    ADD COLUMN system_id UUID REFERENCES game_systems(id) ON DELETE SET NULL;
