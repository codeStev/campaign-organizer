package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.UUID;

public interface DeleteArticleUseCase {

    void delete(UUID worldId, UUID articleId);
}
