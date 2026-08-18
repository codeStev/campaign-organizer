package com.campaignorganizer.campaign.application.arc.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read arcs from sibling aggregates and other contexts (usage, packet, export). */
public interface ArcQueryPort {

    List<ArcView> findByCampaign(UUID campaignId);

    Optional<ArcView> findById(UUID arcId);

    boolean existsInCampaign(UUID arcId, UUID campaignId);
}
