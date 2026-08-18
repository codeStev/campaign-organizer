package com.campaignorganizer.worldbuilding.adapter.timeline.out.context;

import com.campaignorganizer.worldbuilding.application.timeline.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TimelineArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleQueryPort articles;

    public TimelineArticleExistsAdapter(ArticleQueryPort articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }
}
