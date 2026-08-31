package com.campaignorganizer.campaign.adapter.clock.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClockJpaRepository extends JpaRepository<ClockJpaEntity, UUID> {

    List<ClockJpaEntity> findByCampaignIdOrderByPositionAscCreatedAtAsc(UUID campaignId);

    Optional<ClockJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);
}
