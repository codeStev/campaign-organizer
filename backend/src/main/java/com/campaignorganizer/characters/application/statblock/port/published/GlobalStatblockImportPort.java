package com.campaignorganizer.characters.application.statblock.port.published;

/**
 * Published port: resolves-or-creates a global (catalog) statblock on import
 * (ADR-0061/ADR-0096) — deliberately NOT the standard import-port contract.
 * Every other {@code *ImportPort} in this codebase always mints a fresh id,
 * relying on the caller's id-remap pass so re-importing a backup never
 * collides with data that's still there. Applied naively here, that would
 * recreate a duplicate catalog entry on every re-import of a world backup
 * that references one — defeating the point of a shared catalog. Instead
 * this resolves an existing entry by an exact {@code (systemId, name)} match
 * and reuses its id if found, only inserting a new row (with the id carried
 * on {@code view}) when genuinely new. Mirrors
 * {@code GlobalFieldTemplateImportPort} (ADR-0093).
 */
public interface GlobalStatblockImportPort {

    GlobalStatblockView importOrReuse(GlobalStatblockView view);
}
