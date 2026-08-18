package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.UUID;

public interface RestoreArticleRevisionUseCase {

    ArticleView restore(UUID worldId, UUID articleId, UUID revisionId);
}
