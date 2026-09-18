package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort.ArticleRef;
import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker.LinkRef;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the lowercase {@code [[wiki-link]]} name → article index shared by
 * body rendering and reference resolution, so both agree on how a link
 * resolves: aliases fill gaps first (ADR-0116), then slug, then title takes
 * precedence on collision - unchanged from before aliases existed.
 */
final class ArticleRefIndex {

    private ArticleRefIndex() {
    }

    static Map<String, LinkRef> build(List<ArticleRef> refs, Map<UUID, List<String>> aliasesByArticleId) {
        Map<String, LinkRef> index = new HashMap<>();
        for (ArticleRef ref : refs) {
            LinkRef linkRef = new LinkRef(ref.id(), ref.title());
            for (String alias : aliasesByArticleId.getOrDefault(ref.id(), List.of())) {
                index.putIfAbsent(alias.toLowerCase(Locale.ROOT), linkRef);
            }
        }
        for (ArticleRef ref : refs) {
            index.putIfAbsent(ref.slug().toLowerCase(Locale.ROOT), new LinkRef(ref.id(), ref.title()));
        }
        for (ArticleRef ref : refs) {
            index.put(ref.title().toLowerCase(Locale.ROOT), new LinkRef(ref.id(), ref.title()));
        }
        return index;
    }
}
