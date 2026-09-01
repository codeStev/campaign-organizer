package com.campaignorganizer.characters.application.template.port.published;

/**
 * Published port: resolves-or-creates a global field template on import
 * (ADR-0061/ADR-0093) — deliberately NOT the standard import-port contract.
 * Every other {@code *ImportPort} in this codebase always mints a fresh id,
 * relying on the caller's id-remap pass so re-importing a backup never
 * collides with data that's still there. Applied naively here, that would
 * recreate a duplicate global template on every re-import of a world
 * backup that references one - defeating the point of a shared catalog.
 * Instead this resolves an existing global template by an exact
 * {@code (kind, system, name)} match and reuses its id if found, only
 * inserting a new row (with the id carried on {@code view}) when genuinely
 * new.
 */
public interface GlobalFieldTemplateImportPort {

    GlobalFieldTemplateView importOrReuse(GlobalFieldTemplateView view);
}
