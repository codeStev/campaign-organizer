package com.campaignorganizer.campaign;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArcRepository extends JpaRepository<Arc, UUID> {

    List<Arc> findByCampaignIdOrderByPositionAscCreatedAtAsc(UUID campaignId);

    Optional<Arc> findByIdAndCampaignId(UUID id, UUID campaignId);

    boolean existsByIdAndCampaignId(UUID id, UUID campaignId);
}
