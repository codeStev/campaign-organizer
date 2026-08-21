package com.campaignorganizer.worldbuilding.domain.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Pure domain unit test for Markdown -> HTML rendering (ADR-0054). */
class MarkdownRendererTest {

    private final MarkdownRenderer renderer = new MarkdownRenderer();

    @Test
    void rendersHeading() {
        assertThat(renderer.render("# Hello")).contains("<h1>Hello</h1>");
    }

    @Test
    void rendersBold() {
        assertThat(renderer.render("**strong**")).contains("<strong>strong</strong>");
    }

    @Test
    void rendersGfmTable() {
        String md = "| A | B |\n| --- | --- |\n| 1 | 2 |\n";
        assertThat(renderer.render(md)).contains("<table>");
    }

    @Test
    void rendersStrikethroughAndTaskList() {
        assertThat(renderer.render("~~gone~~")).contains("<del>gone</del>");
        assertThat(renderer.render("- [x] done")).contains("checked");
    }

    @Test
    void passesRawImageWithWidthThrough() {
        String md = "before\n\n<img src=\"/api/media/abc/content\" width=\"480\">\n\nafter";
        assertThat(renderer.render(md)).contains("<img src=\"/api/media/abc/content\" width=\"480\">");
    }

    @Test
    void passesRawScriptThroughUnescaped() {
        // Sanitization happens as a separate step after rendering (ArticleRenderer); the
        // renderer itself must not escape raw HTML, or the sanitizer would have nothing to strip.
        assertThat(renderer.render("<script>alert('x')</script>")).contains("<script>alert('x')</script>");
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertThat(renderer.render(null)).isNull();
        assertThat(renderer.render("")).isEmpty();
    }
}
