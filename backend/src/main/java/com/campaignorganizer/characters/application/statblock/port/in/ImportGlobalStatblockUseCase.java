package com.campaignorganizer.characters.application.statblock.port.in;

import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import java.util.UUID;

/**
 * Imports (copies) a global catalog statblock into a specific world campaign
 * (ADR-0096) — the resulting world-scoped statblock carries no live link back
 * to the catalog entry it came from.
 */
public interface ImportGlobalStatblockUseCase {

    StatblockView importIntoCampaign(UUID globalStatblockId, UUID worldId, UUID campaignId, String nameOverride);
}
