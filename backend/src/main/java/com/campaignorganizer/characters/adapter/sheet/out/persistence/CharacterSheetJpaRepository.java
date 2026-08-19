package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterSheetJpaRepository extends JpaRepository<CharacterSheetJpaEntity, UUID> {

    List<CharacterSheetJpaEntity> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<CharacterSheetJpaEntity> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId,
                                                                                 UUID campaignId);

    List<CharacterSheetJpaEntity> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    Optional<CharacterSheetJpaEntity> findByIdAndWorldId(UUID id, UUID worldId);
}
