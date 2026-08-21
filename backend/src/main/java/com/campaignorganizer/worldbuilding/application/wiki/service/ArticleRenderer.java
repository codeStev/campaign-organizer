package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort.ArticleRef;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.domain.wiki.HtmlSanitizer;
import com.campaignorganizer.worldbuilding.domain.wiki.MarkdownRenderer;
import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker;
import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker.LinkRef;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Renders an article's stored Markdown body to display-ready HTML (ADR-0054):
 * wiki-links first (ADR-0014, while the body is still plain text — running
 * Markdown rendering first can inject tags into a link label before
 * {@link WikiLinker} sees it), then Markdown -> HTML, then sanitized
 * (ADR-0025, moved here from write time since this render already runs on
 * every read).
 */
@Service
public class ArticleRenderer implements ArticleRenderPort {

    private final ArticleRepositoryPort articles;
    private final MarkdownRenderer markdownRenderer = new MarkdownRenderer();
    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    public ArticleRenderer(ArticleRepositoryPort articles) {
        this.articles = articles;
    }

    @Override
    @Transactional(readOnly = true)
    public String renderBody(UUID worldId, String body) {
        if (body == null) {
            return null;
        }
        String linked = body.contains("[[") ? WikiLinker.render(body, index(worldId)) : body;
        return sanitizer.sanitize(markdownRenderer.render(linked));
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
