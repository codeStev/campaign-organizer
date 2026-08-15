package com.campaignorganizer.wiki;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void stripsScriptTags() {
        String out = sanitizer.sanitize("<p>Hi</p><script>alert('x')</script>");
        assertThat(out).contains("<p>Hi</p>");
        assertThat(out).doesNotContain("script");
        assertThat(out).doesNotContain("alert");
    }

    @Test
    void stripsEventHandlerAttributes() {
        String out = sanitizer.sanitize("<p onclick=\"steal()\">click</p>");
        assertThat(out).doesNotContain("onclick");
        assertThat(out).contains("click");
    }

    @Test
    void stripsJavascriptUrlLinks() {
        String out = sanitizer.sanitize("<a href=\"javascript:evil()\">x</a>");
        assertThat(out).doesNotContain("javascript:");
    }

    @Test
    void keepsFormattingAndHeadingsAndLists() {
        String html = "<h2>Title</h2><p><strong>bold</strong> and <em>italic</em></p>"
                + "<ul><li>one</li></ul>";
        assertThat(sanitizer.sanitize(html)).isEqualTo(html);
    }

    @Test
    void keepsOwnMediaImages() {
        String out = sanitizer.sanitize("<img src=\"/api/media/abc/content\" alt=\"map\" />");
        assertThat(out).contains("src=\"/api/media/abc/content\"");
    }

    @Test
    void keepsImageDisplayWidthButNotOtherAttributes() {
        String out = sanitizer.sanitize(
                "<img src=\"/api/media/abc/content\" width=\"480\" style=\"position:fixed\" />");
        assertThat(out).contains("width=\"480\"");
        assertThat(out).doesNotContain("style");
    }

    @Test
    void leavesWikiLinkTokensUntouched() {
        String out = sanitizer.sanitize("<p>See [[Goblin]] and [[Waterdeep|the city]].</p>");
        assertThat(out).contains("[[Goblin]]");
        assertThat(out).contains("[[Waterdeep|the city]]");
    }
}
