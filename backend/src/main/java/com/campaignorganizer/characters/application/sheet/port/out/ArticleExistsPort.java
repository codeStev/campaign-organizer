package com.campaignorganizer.characters.application.sheet.port.out;

import java.util.UUID;

public interface ArticleExistsPort {

    boolean existsInWorld(UUID articleId, UUID worldId);
}
