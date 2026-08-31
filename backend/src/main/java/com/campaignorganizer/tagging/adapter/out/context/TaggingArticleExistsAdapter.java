package com.campaignorganizer.tagging.adapter.out.context;

import com.campaignorganizer.tagging.application.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Article-existence check against the worldbuilding context's published port. */
@Component
public class TaggingArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleQueryPort articles;

    public TaggingArticleExistsAdapter(ArticleQueryPort articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }
}
