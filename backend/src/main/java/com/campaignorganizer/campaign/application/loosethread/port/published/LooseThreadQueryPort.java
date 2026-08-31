package com.campaignorganizer.campaign.application.loosethread.port.published;

import java.util.List;
import java.util.UUID;

/**
 * Published port: read loose threads from sibling aggregates and other
 * contexts. {@code findByCampaign} exists for a future per-campaign
 * dashboard (FR-67, not yet built) to list open threads without walking
 * every session.
 */
public interface LooseThreadQueryPort {

    List<LooseThreadView> findBySession(UUID sessionId);

    List<LooseThreadView> findByCampaign(UUID campaignId);
}
