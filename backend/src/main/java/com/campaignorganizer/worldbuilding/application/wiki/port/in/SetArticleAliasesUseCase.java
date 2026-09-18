package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import java.util.List;
import java.util.UUID;

public interface SetArticleAliasesUseCase {

    /** Replaces the article's whole alias set; returns the resulting aliases,
     * alphabetical (case-insensitive). */
    List<String> set(UUID worldId, UUID articleId, List<String> aliases);
}
