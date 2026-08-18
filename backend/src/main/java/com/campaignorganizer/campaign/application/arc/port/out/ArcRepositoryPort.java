package com.campaignorganizer.campaign.application.arc.port.out;

import com.campaignorganizer.campaign.domain.arc.Arc;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArcRepositoryPort {

    List<Arc> findByCampaign(UUID campaignId);

    Optional<Arc> findByIdAndCampaign(UUID arcId, UUID campaignId);

    Optional<Arc> findById(UUID arcId);

    boolean existsInCampaign(UUID arcId, UUID campaignId);

    Arc save(Arc arc);

    void delete(Arc arc);
}
