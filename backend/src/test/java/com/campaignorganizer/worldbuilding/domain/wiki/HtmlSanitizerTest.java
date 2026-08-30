package com.campaignorganizer.worldbuilding.domain.wiki;

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

    @Test
    void keepsGfmTables() {
        String html = "<table><thead><tr><th>A</th></tr></thead>"
                + "<tbody><tr><td>1</td></tr></tbody></table>";
        String out = sanitizer.sanitize(html);
        assertThat(out).contains("<table>").contains("<th>A</th>").contains("<td>1</td>");
    }

    @Test
    void keepsFencedCodeBlocks() {
        // The sanitizer HTML-entity-encodes text content on the way out (e.g. `=` as
        // `&#61;`) - harmless, browsers render it identically; assert structure, not
        // the exact byte-for-byte text encoding.
        String out = sanitizer.sanitize("<pre><code>const x = 1;\n</code></pre>");
        assertThat(out).contains("<pre>").contains("<code>").contains("const x");
    }

    @Test
    void keepsDisabledTaskListCheckboxesButNotOtherInputTypes() {
        String checkbox = sanitizer.sanitize(
                "<li class=\"task-list-item\">"
                        + "<input type=\"checkbox\" class=\"task-list-item-checkbox\" disabled=\"disabled\" "
                        + "readonly=\"readonly\" /> done</li>");
        assertThat(checkbox).contains("type=\"checkbox\"").contains("disabled").contains("readonly");

        String textInput = sanitizer.sanitize("<input type=\"text\" value=\"steal me\" />");
        assertThat(textInput).doesNotContain("<input");
    }
}
