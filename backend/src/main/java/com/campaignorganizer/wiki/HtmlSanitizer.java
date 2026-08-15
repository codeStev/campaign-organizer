package com.campaignorganizer.wiki;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Component;

/**
 * Sanitizes article body HTML on write to prevent stored XSS (follow-up to
 * ADR-0013 / ADR-0025). Allows the formatting the TipTap editor produces
 * (blocks, formatting, links, images) plus images served from our own media
 * endpoint; strips scripts, event handlers, and unknown elements.
 *
 * <p>Note: `[[wiki-links]]` in the body are plain text, not HTML, so they pass
 * through untouched and are resolved later by {@link AutoLinker}.
 */
@Component
public class HtmlSanitizer {

    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            // Permit relative image URLs (our media endpoint, e.g. /api/media/{id}/content).
            .and(new HtmlPolicyBuilder()
                    .allowUrlProtocols("http", "https")
                    .allowElements("img")
                    .allowAttributes("src").matching(HtmlSanitizer::isAllowedImageSrc).onElements("img")
                    // Persist an editor-chosen display width (px or %); nothing else.
                    .allowAttributes("width").matching(HtmlSanitizer::isAllowedWidth).onElements("img")
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
}
