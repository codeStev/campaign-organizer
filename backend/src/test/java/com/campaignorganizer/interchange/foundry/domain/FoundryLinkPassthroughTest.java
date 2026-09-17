package com.campaignorganizer.interchange.foundry.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.worldbuilding.domain.wiki.HtmlSanitizer;
import com.campaignorganizer.worldbuilding.domain.wiki.MarkdownRenderer;
import org.junit.jupiter.api.Test;

/**
 * Pins a surprising, empirically-confirmed quirk of the shared {@link MarkdownRenderer}
 * (flexmark's autolink/email-obfuscation behavior): it HTML-entity-encodes a bare {@code @}
 * as {@code &#64;}, unconditionally, not just inside actual email-like autolinks. That's
 * invisible to every other pushed document (none of them contain a literal {@code @}), but
 * it would silently break Foundry's {@code @UUID[...]} content-link enricher syntax, which
 * matches the literal {@code @} character in raw HTML, not its entity-encoded form —
 * {@link com.campaignorganizer.interchange.foundry.application.service.FoundryPushService}
 * works around this with a targeted {@code "&#64;UUID[" -> "@UUID["} unescape immediately
 * after calling {@code markdownToHtml} (see its session-guide push method), not here. This
 * test exists so a future flexmark/MarkdownRenderer change to this behavior fails loudly
 * instead of silently breaking every Foundry content-link again.
 */
class FoundryLinkPassthroughTest {

    @Test
    void markdownRendererEntityEncodesTheAtSignInFoundryLinkSyntax() {
        String markdown = "A pack of wolves. @UUID[JournalEntry.abc123DEF4567890]{Old Empire}\n\n"
                + "**References:** @UUID[RollTable.xyz789ghijklmno0]{Coastal Road Encounters}";
        MarkdownRenderer renderer = new MarkdownRenderer();
        HtmlSanitizer sanitizer = new HtmlSanitizer();
        String html = sanitizer.sanitize(renderer.render(markdown));

        assertThat(html).doesNotContain("@UUID[");
        assertThat(html).contains("&#64;UUID[JournalEntry.abc123DEF4567890]{Old Empire}");
        assertThat(html).contains("&#64;UUID[RollTable.xyz789ghijklmno0]{Coastal Road Encounters}");
        // The workaround itself: confirms the exact string FoundryPushService's .replace(...) targets.
        assertThat(html.replace("&#64;UUID[", "@UUID[")).contains("@UUID[JournalEntry.abc123DEF4567890]{Old Empire}");
    }
}
