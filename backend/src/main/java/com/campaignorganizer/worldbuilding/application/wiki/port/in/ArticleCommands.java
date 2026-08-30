package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplate;
import java.util.UUID;

public final class ArticleCommands {

    private ArticleCommands() {
    }

    /** {@code slug} is the caller-supplied slug (may be null/blank to derive from the title). */
    public record CreateArticleCommand(UUID worldId, UUID categoryId, UUID parentArticleId, String title,
                                       String slug, ArticleTemplate template, String body) {
    }

    public record UpdateArticleCommand(UUID worldId, UUID articleId, UUID categoryId,
                                       UUID parentArticleId, String title, String slug,
                                       ArticleTemplate template, String body) {
    }
}
