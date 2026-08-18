package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import java.util.Set;
import java.util.UUID;

/** ACL into the interchange/usage context: article ids referenced by a campaign (ADR-0033). */
public interface CampaignArticleUsagePort {

    Set<UUID> articleIdsUsedInCampaign(UUID worldId, UUID campaignId);
}
