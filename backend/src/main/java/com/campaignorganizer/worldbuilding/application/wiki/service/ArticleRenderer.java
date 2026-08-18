package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort.ArticleRef;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker;
import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker.LinkRef;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves wiki-links (ADR-0014) by building the world's article index, then delegating to the domain. */
@Service
public class ArticleRenderer implements ArticleRenderPort {

    private final ArticleRepositoryPort articles;

    public ArticleRenderer(ArticleRepositoryPort articles) {
        this.articles = articles;
    }

    @Override
    @Transactional(readOnly = true)
    public String renderBody(UUID worldId, String body) {
        if (body == null || !body.contains("[[")) {
            return body;
        }
        return WikiLinker.render(body, index(worldId));
    }

    @Override
    public Set<String> linkTargets(String body) {
        return WikiLinker.linkTargets(body);
    }

    private Map<String, LinkRef> index(UUID worldId) {
        var refs = articles.findRefsByWorld(worldId);
        Map<String, LinkRef> index = new HashMap<>();
        // Title takes precedence over slug on collision; slug fills gaps.
        for (ArticleRef ref : refs) {
            index.putIfAbsent(ref.slug().toLowerCase(Locale.ROOT), new LinkRef(ref.id(), ref.title()));
        }
        for (ArticleRef ref : refs) {
            index.put(ref.title().toLowerCase(Locale.ROOT), new LinkRef(ref.id(), ref.title()));
        }
        return index;
    }
}
