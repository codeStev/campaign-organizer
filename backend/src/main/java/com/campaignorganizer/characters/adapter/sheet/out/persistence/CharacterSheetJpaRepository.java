package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CharacterSheetJpaRepository extends JpaRepository<CharacterSheetJpaEntity, UUID> {

    List<CharacterSheetJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<CharacterSheetJpaEntity> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId,
                                                                                 UUID campaignId);

    List<CharacterSheetJpaEntity> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    Optional<CharacterSheetJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);

    boolean existsByGlobalTemplateId(UUID globalTemplateId);

    /** Executed immediately (not deferred to flush) — see CampaignPlayerJpaRepository for why. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE CharacterSheetJpaEntity s SET s.worldTemplateId = NULL, s.globalTemplateId = :globalTemplateId "
            + "WHERE s.worldTemplateId = :worldTemplateId")
    void repointWorldTemplateToGlobal(@Param("worldTemplateId") UUID worldTemplateId,
                                      @Param("globalTemplateId") UUID globalTemplateId);
}
