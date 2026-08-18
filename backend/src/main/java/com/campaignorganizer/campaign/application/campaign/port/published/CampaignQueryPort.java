package com.campaignorganizer.campaign.application.campaign.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read campaigns from sibling aggregates and other contexts (usage, export, sheet). */
public interface CampaignQueryPort {

    List<CampaignView> findByWorld(UUID worldId);

    Optional<CampaignView> findByIdInWorld(UUID campaignId, UUID worldId);

    Optional<CampaignView> findById(UUID campaignId);

    boolean existsInWorld(UUID campaignId, UUID worldId);
}
