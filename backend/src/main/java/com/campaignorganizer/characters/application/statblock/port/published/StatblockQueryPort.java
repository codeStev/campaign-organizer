package com.campaignorganizer.characters.application.statblock.port.published;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Published port: read statblocks from other contexts (campaign, usage, export). */
public interface StatblockQueryPort {

    List<StatblockView> findByWorld(UUID worldId);

    List<StatblockView> findByWorldAndCampaign(UUID worldId, UUID campaignId);

    List<StatblockView> findByWorldAndArticle(UUID worldId, UUID articleId);

    Optional<StatblockView> findByIdInWorld(UUID statblockId, UUID worldId);

    boolean existsInWorld(UUID statblockId, UUID worldId);
}
