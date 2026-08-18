package com.campaignorganizer.campaign.application.campaign.port.out;

import com.campaignorganizer.campaign.domain.campaign.Campaign;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampaignRepositoryPort {

    List<Campaign> findByWorld(UUID worldId);

    Optional<Campaign> findByIdAndWorld(UUID campaignId, UUID worldId);

    Optional<Campaign> findById(UUID campaignId);

    boolean existsInWorld(UUID campaignId, UUID worldId);

    Campaign save(Campaign campaign);

    void delete(Campaign campaign);
}
