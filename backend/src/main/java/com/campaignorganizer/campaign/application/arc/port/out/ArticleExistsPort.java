package com.campaignorganizer.campaign.application.arc.port.out;

import java.util.UUID;

public interface ArticleExistsPort {

    boolean existsInWorld(UUID articleId, UUID worldId);
}
