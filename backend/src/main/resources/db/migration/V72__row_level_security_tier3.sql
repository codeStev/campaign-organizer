-- ADR-0114 Tier 3: tables scoped two or more hops from a world. Same
-- delegation pattern as Tier 2 (confirmed empirically that RLS composes
-- through nested references) — each table delegates to its own immediate
-- parent's policy rather than re-deriving the full chain up to worlds.
-- Grouped by parent below, in dependency order (a parent's own policy must
-- already exist by the time a child table's policy subquery runs it, which
-- is true for every table here since all of Tier 1/2/3 land before the app
-- ever queries against them for real).

-- Parent: campaigns (itself delegates to worlds, Tier 2)
ALTER TABLE arcs ENABLE ROW LEVEL SECURITY;
ALTER TABLE arcs FORCE ROW LEVEL SECURITY;
CREATE POLICY arcs_owner_isolation ON arcs
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE campaign_calendar_feeds ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_calendar_feeds FORCE ROW LEVEL SECURITY;
CREATE POLICY campaign_calendar_feeds_owner_isolation ON campaign_calendar_feeds
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE campaign_players ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaign_players FORCE ROW LEVEL SECURITY;
CREATE POLICY campaign_players_owner_isolation ON campaign_players
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE clocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE clocks FORCE ROW LEVEL SECURITY;
CREATE POLICY clocks_owner_isolation ON clocks
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE encounters ENABLE ROW LEVEL SECURITY;
ALTER TABLE encounters FORCE ROW LEVEL SECURITY;
CREATE POLICY encounters_owner_isolation ON encounters
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE loose_threads ENABLE ROW LEVEL SECURITY;
ALTER TABLE loose_threads FORCE ROW LEVEL SECURITY;
CREATE POLICY loose_threads_owner_isolation ON loose_threads
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY sessions_owner_isolation ON sessions
  USING (campaign_id IN (SELECT id FROM campaigns));

ALTER TABLE todos ENABLE ROW LEVEL SECURITY;
ALTER TABLE todos FORCE ROW LEVEL SECURITY;
CREATE POLICY todos_owner_isolation ON todos
  USING (campaign_id IN (SELECT id FROM campaigns));

-- Parent: sessions (above)
ALTER TABLE cheat_sheets ENABLE ROW LEVEL SECURITY;
ALTER TABLE cheat_sheets FORCE ROW LEVEL SECURITY;
CREATE POLICY cheat_sheets_owner_isolation ON cheat_sheets
  USING (session_id IN (SELECT id FROM sessions));

ALTER TABLE session_attendance ENABLE ROW LEVEL SECURITY;
ALTER TABLE session_attendance FORCE ROW LEVEL SECURITY;
CREATE POLICY session_attendance_owner_isolation ON session_attendance
  USING (session_id IN (SELECT id FROM sessions));

-- Parent: arcs (above)
ALTER TABLE arc_beats ENABLE ROW LEVEL SECURITY;
ALTER TABLE arc_beats FORCE ROW LEVEL SECURITY;
CREATE POLICY arc_beats_owner_isolation ON arc_beats
  USING (arc_id IN (SELECT id FROM arcs));

-- Parent: arc_beats (above)
ALTER TABLE beat_articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE beat_articles FORCE ROW LEVEL SECURITY;
CREATE POLICY beat_articles_owner_isolation ON beat_articles
  USING (beat_id IN (SELECT id FROM arc_beats));

ALTER TABLE beat_card_decks ENABLE ROW LEVEL SECURITY;
ALTER TABLE beat_card_decks FORCE ROW LEVEL SECURITY;
CREATE POLICY beat_card_decks_owner_isolation ON beat_card_decks
  USING (beat_id IN (SELECT id FROM arc_beats));

ALTER TABLE beat_encounters ENABLE ROW LEVEL SECURITY;
ALTER TABLE beat_encounters FORCE ROW LEVEL SECURITY;
CREATE POLICY beat_encounters_owner_isolation ON beat_encounters
  USING (beat_id IN (SELECT id FROM arc_beats));

ALTER TABLE beat_roll_tables ENABLE ROW LEVEL SECURITY;
ALTER TABLE beat_roll_tables FORCE ROW LEVEL SECURITY;
CREATE POLICY beat_roll_tables_owner_isolation ON beat_roll_tables
  USING (beat_id IN (SELECT id FROM arc_beats));

ALTER TABLE beat_statblocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE beat_statblocks FORCE ROW LEVEL SECURITY;
CREATE POLICY beat_statblocks_owner_isolation ON beat_statblocks
  USING (beat_id IN (SELECT id FROM arc_beats));

-- Parent: encounters (above)
ALTER TABLE encounter_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE encounter_entries FORCE ROW LEVEL SECURITY;
CREATE POLICY encounter_entries_owner_isolation ON encounter_entries
  USING (encounter_id IN (SELECT id FROM encounters));

-- Parent: maps (Tier 2)
ALTER TABLE map_pins ENABLE ROW LEVEL SECURITY;
ALTER TABLE map_pins FORCE ROW LEVEL SECURITY;
CREATE POLICY map_pins_owner_isolation ON map_pins
  USING (map_id IN (SELECT id FROM maps));

-- Parent: articles (Tier 2)
ALTER TABLE article_revisions ENABLE ROW LEVEL SECURITY;
ALTER TABLE article_revisions FORCE ROW LEVEL SECURITY;
CREATE POLICY article_revisions_owner_isolation ON article_revisions
  USING (article_id IN (SELECT id FROM articles));

-- Parent: calendars (Tier 2)
ALTER TABLE calendar_months ENABLE ROW LEVEL SECURITY;
ALTER TABLE calendar_months FORCE ROW LEVEL SECURITY;
CREATE POLICY calendar_months_owner_isolation ON calendar_months
  USING (calendar_id IN (SELECT id FROM calendars));

-- Parent: timelines (Tier 2)
ALTER TABLE timeline_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE timeline_events FORCE ROW LEVEL SECURITY;
CREATE POLICY timeline_events_owner_isolation ON timeline_events
  USING (timeline_id IN (SELECT id FROM timelines));
