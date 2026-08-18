package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArcJpaRepository extends JpaRepository<ArcJpaEntity, UUID> {

    List<ArcJpaEntity> findByCampaignIdOrderByPositionAscCreatedAtAsc(UUID campaignId);

    Optional<ArcJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);

    boolean existsByIdAndCampaignId(UUID id, UUID campaignId);
}
