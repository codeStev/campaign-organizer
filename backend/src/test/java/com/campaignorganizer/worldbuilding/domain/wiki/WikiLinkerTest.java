package com.campaignorganizer.worldbuilding.domain.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import com.campaignorganizer.worldbuilding.domain.wiki.WikiLinker.LinkRef;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for wiki-link resolution (ADR-0014). */
class WikiLinkerTest {

    @Test
    void extractsLowercasedLinkTargets() {
        assertThat(WikiLinker.linkTargets("See [[Goblin]] and [[Waterdeep|the city]]."))
                .containsExactlyInAnyOrder("goblin", "waterdeep");
    }

    @Test
    void resolvesKnownLinkToAnchor() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.render("See [[Goblin]].",
                Map.of("goblin", new LinkRef(id, "Goblin")));

        assertThat(out).contains("<a class=\"wiki-link\" data-article-id=\"" + id + "\"");
        assertThat(out).contains(">Goblin</a>");
    }

    @Test
    void resolvesUnknownLinkToBrokenSpan() {
        String out = WikiLinker.render("See [[Nowhere]].", Map.of());
        assertThat(out).contains("<span class=\"broken-link\">Nowhere</span>");
    }

    @Test
    void usesExplicitLabelWhenProvided() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.render("Visit [[Waterdeep|the city]].",
                Map.of("waterdeep", new LinkRef(id, "Waterdeep")));
        assertThat(out).contains(">the city</a>");
    }

    @Test
    void renderMarkdown_resolvesKnownLinkToBoldPlainText() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.renderMarkdown("See [[Goblin]].",
                Map.of("goblin", new LinkRef(id, "Goblin")));

        assertThat(out).isEqualTo("See **Goblin**.");
    }

    @Test
    void renderMarkdown_resolvesUnknownLinkToItalicPlainText() {
        String out = WikiLinker.renderMarkdown("See [[Nowhere]].", Map.of());
        assertThat(out).isEqualTo("See *Nowhere*.");
    }

    @Test
    void renderMarkdown_usesExplicitLabelWhenProvided() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.renderMarkdown("Visit [[Waterdeep|the city]].",
                Map.of("waterdeep", new LinkRef(id, "Waterdeep")));
        assertThat(out).isEqualTo("Visit **the city**.");
    }

    @Test
    void renderMarkdown_neverHtmlEscapesUnlikeRender() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.renderMarkdown("See [[Goblin|A & B]].",
                Map.of("goblin", new LinkRef(id, "Goblin")));
        assertThat(out).isEqualTo("See **A & B**.");
    }

    // --- [label](target) syntax (ADR-0116) ---

    @Test
    void mdLinkSyntax_resolvesKnownTargetToAnchor() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.render("See [the goblin](Goblin).",
                Map.of("goblin", new LinkRef(id, "Goblin")));

        assertThat(out).contains("<a class=\"wiki-link\" data-article-id=\"" + id + "\"");
        assertThat(out).contains(">the goblin</a>");
    }

    @Test
    void mdLinkSyntax_resolvesUnknownTargetToBrokenSpan() {
        String out = WikiLinker.render("See [nowhere](Nowhere).", Map.of());
        assertThat(out).contains("<span class=\"broken-link\">nowhere</span>");
    }

    @Test
    void mdLinkSyntax_extractsLinkTarget() {
        assertThat(WikiLinker.linkTargets("See [the goblin](Goblin)."))
                .containsExactly("goblin");
    }

    @Test
    void mdLinkSyntax_leavesRealUrlTargetsUntouchedForMarkdownRendererToHandle() {
        String source = "See [our site](https://example.com/goblins) and [a page](/local/path) "
                + "and [an anchor](#section) and [an email](mailto:gm@example.com).";
        assertThat(WikiLinker.render(source, Map.of())).isEqualTo(source);
        assertThat(WikiLinker.linkTargets(source)).isEmpty();
    }

    @Test
    void mdLinkSyntax_doesNotMatchImageSyntax() {
        String source = "![a goblin](Goblin)";
        assertThat(WikiLinker.render(source, Map.of("goblin", new LinkRef(UUID.randomUUID(), "Goblin"))))
                .isEqualTo(source);
        assertThat(WikiLinker.linkTargets(source)).isEmpty();
    }

    @Test
    void mdLinkSyntax_renderMarkdown_resolvesToBoldPlainText() {
        UUID id = UUID.randomUUID();
        String out = WikiLinker.renderMarkdown("See [the goblin](Goblin).",
                Map.of("goblin", new LinkRef(id, "Goblin")));
        assertThat(out).isEqualTo("See **the goblin**.");
    }

    @Test
    void linkedSpansCoversBothSyntaxesRegardlessOfResolution() {
        String body = "[[Goblin]] and [the city](Waterdeep) and [ext](https://example.com)";
        assertThat(WikiLinker.linkedSpans(body)).hasSize(3);
    }
}
