package com.campaignorganizer.characters.application.sheet.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read character sheets from other contexts (usage, export). */
public interface CharacterSheetQueryPort {

    List<CharacterSheetView> findByWorld(UUID worldId);

    List<CharacterSheetView> findByWorldAndCampaign(UUID worldId, UUID campaignId);

    List<CharacterSheetView> findByWorldAndArticle(UUID worldId, UUID articleId);

    Optional<CharacterSheetView> findByIdInWorld(UUID sheetId, UUID worldId);
}
