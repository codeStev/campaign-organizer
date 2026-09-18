package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.List;
import java.util.UUID;

public interface GetArticleAliasesUseCase {

    List<String> get(UUID worldId, UUID articleId);
}
