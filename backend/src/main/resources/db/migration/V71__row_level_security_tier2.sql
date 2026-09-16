-- ADR-0114 Tier 2: tables one hop from a world via a direct, NOT NULL
-- world_id column. Delegates to worlds' own Tier 1 policy rather than
-- re-deriving ownership (confirmed empirically that RLS composes through
-- nested references) — simpler and less error-prone than repeating the
-- owner_id equality check on every table.

ALTER TABLE articles ENABLE ROW LEVEL SECURITY;
ALTER TABLE articles FORCE ROW LEVEL SECURITY;
CREATE POLICY articles_owner_isolation ON articles
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE beat_kinds ENABLE ROW LEVEL SECURITY;
ALTER TABLE beat_kinds FORCE ROW LEVEL SECURITY;
CREATE POLICY beat_kinds_owner_isolation ON beat_kinds
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE calendars ENABLE ROW LEVEL SECURITY;
ALTER TABLE calendars FORCE ROW LEVEL SECURITY;
CREATE POLICY calendars_owner_isolation ON calendars
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE campaigns ENABLE ROW LEVEL SECURITY;
ALTER TABLE campaigns FORCE ROW LEVEL SECURITY;
CREATE POLICY campaigns_owner_isolation ON campaigns
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE card_decks ENABLE ROW LEVEL SECURITY;
ALTER TABLE card_decks FORCE ROW LEVEL SECURITY;
CREATE POLICY card_decks_owner_isolation ON card_decks
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE categories FORCE ROW LEVEL SECURITY;
CREATE POLICY categories_owner_isolation ON categories
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE character_sheets ENABLE ROW LEVEL SECURITY;
ALTER TABLE character_sheets FORCE ROW LEVEL SECURITY;
CREATE POLICY character_sheets_owner_isolation ON character_sheets
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE documents FORCE ROW LEVEL SECURITY;
CREATE POLICY documents_owner_isolation ON documents
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE entity_tags ENABLE ROW LEVEL SECURITY;
ALTER TABLE entity_tags FORCE ROW LEVEL SECURITY;
CREATE POLICY entity_tags_owner_isolation ON entity_tags
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE field_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE field_templates FORCE ROW LEVEL SECURITY;
CREATE POLICY field_templates_owner_isolation ON field_templates
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE handout_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE handout_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY handout_categories_owner_isolation ON handout_categories
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE handouts ENABLE ROW LEVEL SECURITY;
ALTER TABLE handouts FORCE ROW LEVEL SECURITY;
CREATE POLICY handouts_owner_isolation ON handouts
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE map_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE map_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY map_categories_owner_isolation ON map_categories
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE maps ENABLE ROW LEVEL SECURITY;
ALTER TABLE maps FORCE ROW LEVEL SECURITY;
CREATE POLICY maps_owner_isolation ON maps
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE media ENABLE ROW LEVEL SECURITY;
ALTER TABLE media FORCE ROW LEVEL SECURITY;
CREATE POLICY media_owner_isolation ON media
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE players ENABLE ROW LEVEL SECURITY;
ALTER TABLE players FORCE ROW LEVEL SECURITY;
CREATE POLICY players_owner_isolation ON players
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE relationships ENABLE ROW LEVEL SECURITY;
ALTER TABLE relationships FORCE ROW LEVEL SECURITY;
CREATE POLICY relationships_owner_isolation ON relationships
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE roll_tables ENABLE ROW LEVEL SECURITY;
ALTER TABLE roll_tables FORCE ROW LEVEL SECURITY;
CREATE POLICY roll_tables_owner_isolation ON roll_tables
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE sheet_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE sheet_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY sheet_categories_owner_isolation ON sheet_categories
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE statblocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE statblocks FORCE ROW LEVEL SECURITY;
CREATE POLICY statblocks_owner_isolation ON statblocks
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE table_deck_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE table_deck_categories FORCE ROW LEVEL SECURITY;
CREATE POLICY table_deck_categories_owner_isolation ON table_deck_categories
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE timelines ENABLE ROW LEVEL SECURITY;
ALTER TABLE timelines FORCE ROW LEVEL SECURITY;
CREATE POLICY timelines_owner_isolation ON timelines
  USING (world_id IN (SELECT id FROM worlds));

ALTER TABLE whiteboards ENABLE ROW LEVEL SECURITY;
ALTER TABLE whiteboards FORCE ROW LEVEL SECURITY;
CREATE POLICY whiteboards_owner_isolation ON whiteboards
  USING (world_id IN (SELECT id FROM worlds));
