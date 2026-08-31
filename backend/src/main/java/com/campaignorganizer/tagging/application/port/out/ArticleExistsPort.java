package com.campaignorganizer.tagging.application.port.out;

import java.util.UUID;

public interface ArticleExistsPort {

    boolean existsInWorld(UUID articleId, UUID worldId);
}
