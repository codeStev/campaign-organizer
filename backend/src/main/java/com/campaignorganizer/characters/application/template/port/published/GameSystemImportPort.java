package com.campaignorganizer.characters.application.template.port.published;

/**
 * Published port: resolves-or-creates a game system on import (ADR-0061/
 * ADR-0094) — same non-standard, deliberate contract as
 * {@link GlobalFieldTemplateImportPort} and for the same reason: minting a
 * fresh id on every import would fragment what should be one shared system
 * across re-imports. Resolves an existing game system by exact
 * case-insensitive name match and reuses its id if found, only inserting a
 * new row (with the id carried on {@code view}) when genuinely new.
 */
public interface GameSystemImportPort {

    GameSystemView importOrReuse(GameSystemView view);
}
