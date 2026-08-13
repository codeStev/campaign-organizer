package com.campaignorganizer.sheet;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterSheetRepository extends JpaRepository<CharacterSheet, UUID> {

    List<CharacterSheet> findByWorldIdOrderByCreatedAtDesc(UUID worldId);

    List<CharacterSheet> findByWorldIdAndCampaignIdOrderByCreatedAtDesc(UUID worldId, UUID campaignId);

    List<CharacterSheet> findByWorldIdAndArticleId(UUID worldId, UUID articleId);

    Optional<CharacterSheet> findByIdAndWorldId(UUID id, UUID worldId);
}
