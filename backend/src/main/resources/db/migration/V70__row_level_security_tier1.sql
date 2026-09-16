-- ADR-0114 Tier 1: direct-owner_id tables. FORCE isn't load-bearing for
-- app_runtime (it's not the table owner — app is), but is set anyway as a
-- backstop for anyone ever connecting as the superuser directly.

ALTER TABLE worlds ENABLE ROW LEVEL SECURITY;
ALTER TABLE worlds FORCE ROW LEVEL SECURITY;
CREATE POLICY worlds_owner_isolation ON worlds
  USING (owner_id = current_setting('app.current_account_id', true)::uuid);

ALTER TABLE game_systems ENABLE ROW LEVEL SECURITY;
ALTER TABLE game_systems FORCE ROW LEVEL SECURITY;
CREATE POLICY game_systems_owner_isolation ON game_systems
  USING (owner_id = current_setting('app.current_account_id', true)::uuid);

ALTER TABLE global_field_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE global_field_templates FORCE ROW LEVEL SECURITY;
CREATE POLICY global_field_templates_owner_isolation ON global_field_templates
  USING (owner_id = current_setting('app.current_account_id', true)::uuid);

ALTER TABLE global_statblocks ENABLE ROW LEVEL SECURITY;
ALTER TABLE global_statblocks FORCE ROW LEVEL SECURITY;
CREATE POLICY global_statblocks_owner_isolation ON global_statblocks
  USING (owner_id = current_setting('app.current_account_id', true)::uuid);

ALTER TABLE ai_provider_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE ai_provider_settings FORCE ROW LEVEL SECURITY;
CREATE POLICY ai_provider_settings_owner_isolation ON ai_provider_settings
  USING (owner_id = current_setting('app.current_account_id', true)::uuid);
