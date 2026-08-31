package com.campaignorganizer.campaign.adapter.loosethread.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LooseThreadJpaRepository extends JpaRepository<LooseThreadJpaEntity, UUID> {

    List<LooseThreadJpaEntity> findBySessionIdOrderByCreatedAtDesc(UUID sessionId);

    List<LooseThreadJpaEntity> findByCampaignIdOrderByCreatedAtDesc(UUID campaignId);

    Optional<LooseThreadJpaEntity> findByIdAndSessionId(UUID id, UUID sessionId);
}
