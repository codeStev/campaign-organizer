package com.campaignorganizer.characters.adapter.statblock.out.context;

import com.campaignorganizer.characters.application.statblock.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves article existence for the statblock module via the wiki article query port. */
@Component
public class StatblockArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleQueryPort articles;

    public StatblockArticleExistsAdapter(ArticleQueryPort articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }
}
