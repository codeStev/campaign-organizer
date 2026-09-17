package com.campaignorganizer.worldbuilding.application.wiki.port.published;

import java.util.Set;
import java.util.UUID;

/** Published port: resolve wiki-links in an article body (ADR-0014) for other contexts. */
public interface ArticleRenderPort {

    /** Render {@code body}, resolving {@code [[links]]} against the world's articles. */
    String renderBody(UUID worldId, String body);

    /** Render {@code body} for an external, non-HTML consumer (ADR-0115): resolve
     * {@code [[links]]} against the world's articles into plain Markdown emphasis
     * instead of HTML anchors, and skip Markdown-to-HTML rendering and sanitization
     * entirely — the result stays valid Markdown end to end. */
    String renderBodyAsMarkdown(UUID worldId, String body);

    /** Lowercased {@code [[target]]} names referenced in a body (for backlink detection). */
    Set<String> linkTargets(String body);

    /** Converts already wiki-link-resolved Markdown to sanitized HTML, with no wiki-link
     * handling of its own (ADR-0115 correction) — for external consumers (Foundry
     * JournalEntry pages) whose display actually reads a page's {@code content} field, not
     * its {@code markdown} source; {@code markdown} only feeds Foundry's own Markdown
     * *editor* when a page is opened for editing there, so pushing markdown alone left
     * every pushed page rendering empty. Call this on the same Markdown string already
     * built via {@link #renderBodyAsMarkdown}, after any further rewriting (e.g. embedded
     * media paths), so both fields describe the exact same content. */
    String markdownToHtml(String markdown);
}
