package com.campaignorganizer.worldbuilding.adapter.relationship.out.context;

import com.campaignorganizer.worldbuilding.application.relationship.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Checks article existence via the wiki article query port (ADR-0050). */
@Component
public class RelationshipArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleQueryPort articles;

    public RelationshipArticleExistsAdapter(ArticleQueryPort articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }
}
