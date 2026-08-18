package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatblockJpaRepository extends JpaRepository<StatblockJpaEntity, UUID> {

    List<StatblockJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<StatblockJpaEntity> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId, UUID campaignId);

    List<StatblockJpaEntity> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    Optional<StatblockJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
