package com.campaignorganizer.campaign.adapter.encounter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncounterJpaRepository extends JpaRepository<EncounterJpaEntity, UUID> {

    List<EncounterJpaEntity> findByCampaignIdOrderByCreatedAtAsc(UUID campaignId);

    Optional<EncounterJpaEntity> findByIdAndCampaignId(UUID id, UUID campaignId);
}
