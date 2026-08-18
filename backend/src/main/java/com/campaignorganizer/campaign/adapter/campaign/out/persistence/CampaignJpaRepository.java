package com.campaignorganizer.campaign.adapter.campaign.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignJpaRepository extends JpaRepository<CampaignJpaEntity, UUID> {

    List<CampaignJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    Optional<CampaignJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByIdAndWorldId(UUID id, UUID worldId);
}
