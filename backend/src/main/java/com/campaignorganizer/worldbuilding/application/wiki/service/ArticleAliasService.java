package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.GetArticleAliasesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.SetArticleAliasesUseCase;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleAliasRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Article alias read/write use cases (ADR-0116) — mirrors the tags
 * feature's replace-all shape, scoped to articles only (aliases are
 * intrinsic to an article's own identity, unlike freeform folksonomy tags,
 * so this stays entirely inside {@code worldbuilding} rather than routing
 * through a separate bounded context). */
@Service
public class ArticleAliasService implements GetArticleAliasesUseCase, SetArticleAliasesUseCase {

    private final ArticleAliasRepositoryPort aliases;
    private final ArticleRepositoryPort articles;

    public ArticleAliasService(ArticleAliasRepositoryPort aliases, ArticleRepositoryPort articles) {
        this.aliases = aliases;
        this.articles = articles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> get(UUID worldId, UUID articleId) {
        requireArticle(worldId, articleId);
        return aliases.findByArticle(worldId, articleId);
    }

    @Override
    @Transactional
    public List<String> set(UUID worldId, UUID articleId, List<String> rawAliases) {
        requireArticle(worldId, articleId);
        // Case-insensitive de-dupe, first-seen casing kept - unlike tags,
        // an alias is a proper name an author typed deliberately, so its
        // casing is preserved rather than folded to lowercase.
        Map<String, String> byLowercase = new LinkedHashMap<>();
        for (String raw : rawAliases) {
            if (raw == null) {
                continue;
            }
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            byLowercase.putIfAbsent(trimmed.toLowerCase(), trimmed);
        }
        List<String> normalized = byLowercase.values().stream()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
        this.aliases.replaceAll(worldId, articleId, normalized);
        return normalized;
    }

    private void requireArticle(UUID worldId, UUID articleId) {
        if (!articles.existsInWorld(articleId, worldId)) {
            throw new NotFoundException("Article not found");
        }
    }
}
