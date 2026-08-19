package com.campaignorganizer.characters.application.sheet.port.out;

import com.campaignorganizer.characters.domain.sheet.CharacterSheet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CharacterSheetRepositoryPort {

    List<CharacterSheet> findByWorld(UUID worldId);

    List<CharacterSheet> findByWorldAndCampaign(UUID worldId, UUID campaignId);

    List<CharacterSheet> findByWorldAndArticle(UUID worldId, UUID articleId);

    Optional<CharacterSheet> findByIdAndWorld(UUID sheetId, UUID worldId);

    CharacterSheet save(CharacterSheet sheet);

    void delete(CharacterSheet sheet);
}
