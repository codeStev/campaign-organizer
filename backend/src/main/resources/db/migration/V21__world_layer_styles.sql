-- Persisted per-layer map styling (colour + icon), keyed by layer name, so it
-- travels with the world (export, cross-device) instead of living in the browser
-- (ADR-0049).
ALTER TABLE worlds ADD COLUMN layer_styles JSONB NOT NULL DEFAULT '{}'::jsonb;
