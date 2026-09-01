package com.campaignorganizer.campaign.application.encounter.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read encounters from sibling modules (beat linkage) and other contexts (packet, export). */
public interface EncounterQueryPort {

    List<EncounterView> findByCampaign(UUID campaignId);

    Optional<EncounterView> findById(UUID encounterId);

    boolean existsInCampaign(UUID encounterId, UUID campaignId);
}
