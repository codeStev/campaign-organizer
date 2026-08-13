package com.campaignorganizer.statblock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatblockRepository extends JpaRepository<Statblock, UUID> {

    List<Statblock> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<Statblock> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId, UUID campaignId);

    List<Statblock> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    Optional<Statblock> findByIdAndWorldId(UUID id, UUID worldId);
}
