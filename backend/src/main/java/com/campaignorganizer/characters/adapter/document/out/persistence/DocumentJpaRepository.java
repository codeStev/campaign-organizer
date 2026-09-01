package com.campaignorganizer.characters.adapter.document.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, UUID> {

    List<DocumentJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<DocumentJpaEntity> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId, UUID campaignId);

    Optional<DocumentJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
