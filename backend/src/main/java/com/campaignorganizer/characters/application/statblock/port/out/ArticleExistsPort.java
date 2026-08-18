package com.campaignorganizer.characters.application.statblock.port.out;

import java.util.UUID;

public interface ArticleExistsPort {

    boolean existsInWorld(UUID articleId, UUID worldId);
}
