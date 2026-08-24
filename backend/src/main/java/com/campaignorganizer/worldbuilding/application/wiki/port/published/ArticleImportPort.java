package com.campaignorganizer.worldbuilding.application.wiki.port.published;

/**
 * Published port: persists an article exactly as given (id, foreign keys, and
 * timestamps already resolved by the caller) instead of generating a new id —
 * backup import's counterpart to the normal create flow (ADR-0061).
 */
public interface ArticleImportPort {

    ArticleView importArticle(ArticleView view);
}
