package com.campaignorganizer.characters.adapter.sheet.out.context;

import com.campaignorganizer.characters.application.sheet.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** ACL: resolves article existence for the sheet module via the wiki article query port. */
@Component
public class SheetArticleExistsAdapter implements ArticleExistsPort {

    private final ArticleQueryPort articles;

    public SheetArticleExistsAdapter(ArticleQueryPort articles) {
        this.articles = articles;
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return articles.existsInWorld(articleId, worldId);
    }
}
