package com.campaignorganizer.campaign.application.session.port.out;

import com.campaignorganizer.campaign.domain.session.Session;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepositoryPort {

    List<Session> findOrdered(UUID campaignId);

    Optional<Session> findByIdAndCampaign(UUID sessionId, UUID campaignId);

    Session save(Session session);

    void delete(Session session);
}
