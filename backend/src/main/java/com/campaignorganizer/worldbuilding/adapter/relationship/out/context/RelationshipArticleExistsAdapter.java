package com.campaignorganizer.worldbuilding.adapter.relationship.out.context;

import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.ArticleExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Checks article existence against the (still-legacy) article store. Once the
 * article aggregate is modelled inside this context, this becomes a direct call —
 * the port stays the same (ADR-0050).
 */
@Component
public class RelationshipArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleRepository articles;

    public RelationshipArticleExistsAdapter(ArticleRepository articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsByIdAndWorldId(articleId, worldId);
    }
}
