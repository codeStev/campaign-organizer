package com.campaignorganizer.worldbuilding.adapter.map.out.context;

import com.campaignorganizer.worldbuilding.application.map.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves article existence for the map module via the wiki article query port. */
@Component
public class MapArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleQueryPort articles;

    public MapArticleExistsAdapter(ArticleQueryPort articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }
}
