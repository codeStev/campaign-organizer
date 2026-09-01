package com.campaignorganizer.campaign.adapter.todo.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoJpaRepository extends JpaRepository<TodoJpaEntity, UUID> {

    List<TodoJpaEntity> findByCampaignIdAndSessionIdIsNullOrderByCreatedAtAsc(UUID campaignId);

    List<TodoJpaEntity> findBySessionIdOrderByCreatedAtAsc(UUID sessionId);

    List<TodoJpaEntity> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);

    Optional<TodoJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);
}
