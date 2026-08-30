package com.campaignorganizer.worldbuilding.domain.wiki;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;

/**
 * Sanitizes an article's rendered HTML before display, the only place raw
 * HTML ever reaches the browser (ADR-0054, superseding the write-time timing
 * of ADR-0025). Allows the formatting a Markdown render produces (blocks,
 * formatting, links, images) plus images served from our own media endpoint
 * and any raw HTML an author embedded directly (e.g. a sized {@code <img>});
 * strips scripts, event handlers, and unknown elements.
 */
public final class HtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            // GFM tables (ADR-0076) - table/tr/td/th/thead/tbody/etc.
            .and(Sanitizers.TABLES)
            // Permit relative image URLs (our media endpoint, e.g. /api/media/{id}/content).
            .and(new HtmlPolicyBuilder()
                    .allowUrlProtocols("http", "https")
                    .allowElements("img")
                    .allowAttributes("src").matching(HtmlSanitizer::isAllowedImageSrc).onElements("img")
                    // Persist an editor-chosen display width (px or %); nothing else.
                    .allowAttributes("width").matching(HtmlSanitizer::isAllowedWidth).onElements("img")
                    .toFactory())
            // Wiki-link anchors and broken-link markers WikiLinker injects (ADR-0014) —
            // this now runs after link resolution (ADR-0054), so its own output must be
            // explicitly allowed like any other HTML reaching this policy.
            .and(new HtmlPolicyBuilder()
                    .allowElements("a", "span")
                    .allowAttributes("class").matching(HtmlSanitizer::isAllowedLinkClass).onElements("a", "span")
                    .allowAttributes("data-article-id").matching(HtmlSanitizer::isUuid).onElements("a")
                    .toFactory())
            // Fenced code blocks (ADR-0076) - FORMATTING already allows <code>, not <pre>.
            .and(new HtmlPolicyBuilder()
                    .allowElements("pre")
                    .toFactory())
            // Task-list checkboxes (ADR-0076, GFM task lists via flexmark's TaskListExtension) -
            // always rendered disabled/readonly (non-interactive, no form to submit into), so
            // this doesn't reopen the form-injection risk <input> is normally excluded to avoid.
            .and(new HtmlPolicyBuilder()
                    .allowElements("input")
                    .allowAttributes("type").matching(HtmlSanitizer::isCheckboxType).onElements("input")
                    .allowAttributes("disabled", "readonly").onElements("input")
                    .allowAttributes("class").matching(HtmlSanitizer::isAllowedTaskListClass).onElements("input")
                    .toFactory());

    public String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        return POLICY.sanitize(html);
    }

    private static boolean isAllowedImageSrc(String src) {
        return src.startsWith("/api/media/") || src.startsWith("http://") || src.startsWith("https://");
    }

    /** A bare pixel width, e.g. "480". (Percent is not accepted by the images policy.) */
    private static boolean isAllowedWidth(String width) {
        return width.matches("\\d{1,4}");
    }

    private static boolean isAllowedLinkClass(String value) {
        return "wiki-link".equals(value) || "broken-link".equals(value);
    }

    private static boolean isCheckboxType(String value) {
        return "checkbox".equals(value);
    }

    private static boolean isAllowedTaskListClass(String value) {
        return "task-list-item-checkbox".equals(value);
    }

    private static boolean isUuid(String value) {
        return value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
    }
}
