package com.campaignorganizer.campaign.application.session.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read sessions from sibling aggregates and other contexts (packet, beat, export). */
public interface SessionQueryPort {

    List<SessionView> findOrdered(UUID campaignId);

    Optional<SessionView> findByIdInCampaign(UUID sessionId, UUID campaignId);

    /** Looks up a session without knowing its campaign (its campaignId is on the view). */
    Optional<SessionView> findById(UUID sessionId);

    boolean existsInCampaign(UUID sessionId, UUID campaignId);
}
