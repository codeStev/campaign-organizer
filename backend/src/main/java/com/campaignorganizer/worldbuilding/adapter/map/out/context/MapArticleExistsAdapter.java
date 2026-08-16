package com.campaignorganizer.worldbuilding.adapter.map.out.context;

import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.worldbuilding.application.map.port.out.ArticleExistsPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves article existence for the map module via the legacy wiki repository. */
@Component
public class MapArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleRepository articles;

    public MapArticleExistsAdapter(ArticleRepository articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsByIdAndWorldId(articleId, worldId);
    }
}
