package com.campaignorganizer.worldbuilding.application.map.port.out;

import java.util.UUID;

public interface ArticleExistsPort {

    boolean existsInWorld(UUID articleId, UUID worldId);
}
