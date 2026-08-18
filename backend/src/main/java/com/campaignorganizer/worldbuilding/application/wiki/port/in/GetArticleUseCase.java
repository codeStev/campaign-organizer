package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.UUID;

public interface GetArticleUseCase {

    ArticleView get(UUID worldId, UUID articleId);
}
