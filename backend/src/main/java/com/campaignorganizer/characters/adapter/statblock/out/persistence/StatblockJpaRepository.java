package com.campaignorganizer.characters.adapter.statblock.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StatblockJpaRepository extends JpaRepository<StatblockJpaEntity, UUID> {

    List<StatblockJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<StatblockJpaEntity> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId, UUID campaignId);

    List<StatblockJpaEntity> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    Optional<StatblockJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByGlobalTemplateId(UUID globalTemplateId);

    /** Executed immediately (not deferred to flush) — see CampaignPlayerJpaRepository for why. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE StatblockJpaEntity s SET s.worldTemplateId = NULL, s.globalTemplateId = :globalTemplateId "
            + "WHERE s.worldTemplateId = :worldTemplateId")
    void repointWorldTemplateToGlobal(@Param("worldTemplateId") UUID worldTemplateId,
                                      @Param("globalTemplateId") UUID globalTemplateId);
}
