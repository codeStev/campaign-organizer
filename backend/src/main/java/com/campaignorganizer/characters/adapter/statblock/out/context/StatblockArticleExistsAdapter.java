package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.characters.application.statblock.port.out.ArticleExistsPort;
import com.campaignorganizer.wiki.ArticleRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves article existence for the statblock module via the legacy wiki repository. */
@Component
public class StatblockArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleRepository articles;

    public StatblockArticleExistsAdapter(ArticleRepository articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsByIdAndWorldId(articleId, worldId);
    }
}
