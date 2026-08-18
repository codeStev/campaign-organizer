package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.util.Set;
import java.util.UUID;

/** Published port: resolve wiki-links in an article body (ADR-0014) for other contexts. */
public interface ArticleRenderPort {

    /** Render {@code body}, resolving {@code [[links]]} against the world's articles. */
    String renderBody(UUID worldId, String body);

    /** Lowercased {@code [[target]]} names referenced in a body (for backlink detection). */
    Set<String> linkTargets(String body);
}
