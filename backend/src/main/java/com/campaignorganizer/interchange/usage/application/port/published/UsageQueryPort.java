package com.campaignorganizer.interchange.usage.application.port.published;

import java.util.Set;
import java.util.UUID;

/**
 * Published port: derived usage queries other contexts need (ADR-0033). Wiki's
 * article listing uses this for its campaign-scoped filter, since "articles a
 * campaign references" spans beats, sheets, and statblocks — data interchange
 * alone can see across.
 */
public interface UsageQueryPort {

    /** Article ids referenced by a campaign's play content (beats, sheets, statblocks). */
    Set<UUID> articleIdsUsedInCampaign(UUID worldId, UUID campaignId);
}
