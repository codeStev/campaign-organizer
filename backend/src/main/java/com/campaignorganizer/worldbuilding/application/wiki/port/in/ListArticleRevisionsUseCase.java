package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRevisionView;
import java.util.List;
import java.util.UUID;

public interface ListArticleRevisionsUseCase {

    List<ArticleRevisionView> list(UUID worldId, UUID articleId);
}
