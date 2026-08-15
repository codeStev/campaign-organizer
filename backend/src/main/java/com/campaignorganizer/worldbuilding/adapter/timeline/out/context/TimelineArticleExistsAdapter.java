package com.campaignorganizer.worldbuilding.adapter.timeline.out.context;

import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.worldbuilding.application.timeline.port.out.ArticleExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TimelineArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleRepository articles;

    public TimelineArticleExistsAdapter(ArticleRepository articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsByIdAndWorldId(articleId, worldId);
    }
}
