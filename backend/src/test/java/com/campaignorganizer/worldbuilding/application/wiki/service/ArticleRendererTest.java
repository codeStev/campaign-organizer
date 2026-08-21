package com.campaignorganizer.worldbuilding.application.wiki.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort.ArticleRef;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit test for the full Markdown -> HTML article render pipeline (ADR-0054). */
@ExtendWith(MockitoExtension.class)
class ArticleRendererTest {

    @Mock
    private ArticleRepositoryPort articles;

    private ArticleRenderer renderer;
    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        renderer = new ArticleRenderer(articles);
    }

    @Test
    void rendersPlainMarkdownWithoutQueryingLinkIndex() {
        String out = renderer.renderBody(worldId, "# Title\n\n**bold** text.");
        assertThat(out).contains("<h1>Title</h1>").contains("<strong>bold</strong>");
    }

    @Test
    void resolvesWikiLinkThenRendersSurroundingMarkdown() {
        UUID goblinId = UUID.randomUUID();
        when(articles.findRefsByWorld(worldId)).thenReturn(
                List.of(new ArticleRef(goblinId, "goblin", "Goblin")));

        String out = renderer.renderBody(worldId, "See **[[Goblin]]** nearby.");

        assertThat(out).contains("<strong>");
        assertThat(out).contains("class=\"wiki-link\"");
        assertThat(out).contains("data-article-id=\"" + goblinId + "\"");
        assertThat(out).contains(">Goblin</a>");
    }

    @Test
    void linkLabelWithMarkdownMetacharactersNeverLeaksEscapedTags() {
        UUID goblinId = UUID.randomUUID();
        when(articles.findRefsByWorld(worldId)).thenReturn(
                List.of(new ArticleRef(goblinId, "goblin", "Goblin")));

        // WikiLinker must run before Markdown rendering. If Markdown ran first, "*elite*"
        // in the label would already be "<em>elite</em>" by the time WikiLinker's permissive
        // label regex matches across it, and its label-escaping step would turn those tags
        // into visible literal text ("&lt;em&gt;") instead of a clean rendered link.
        String out = renderer.renderBody(worldId, "You encounter [[Goblin|the *elite* goblins]] nearby.");

        assertThat(out).doesNotContain("&lt;em&gt;").doesNotContain("&lt;/em&gt;");
        assertThat(out).contains("class=\"wiki-link\"");
        assertThat(out).contains("data-article-id=\"" + goblinId + "\"");
    }

    @Test
    void sanitizesScriptFromRenderedOutput() {
        String out = renderer.renderBody(worldId, "safe\n\n<script>alert('x')</script>");
        assertThat(out).doesNotContain("<script>");
    }

    @Test
    void imageWithWidthRendersRawHtmlThroughMarkdown() {
        // The sanitizer (which now runs on this output) re-serializes HTML in its own
        // normalized form (e.g. self-closing "<img ... />"), so match on the attributes
        // surviving intact rather than an exact byte-for-byte tag string.
        String out = renderer.renderBody(worldId,
                "before\n\n<img src=\"/api/media/abc/content\" width=\"480\">\n\nafter");
        assertThat(out).contains("src=\"/api/media/abc/content\"").contains("width=\"480\"");
    }

    @Test
    void nullBodyRendersToNull() {
        assertThat(renderer.renderBody(worldId, null)).isNull();
    }
}
