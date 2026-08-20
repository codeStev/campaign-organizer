package com.campaignorganizer.worldbuilding.domain.wiki;

import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.List;

/**
 * Renders an article's Markdown body to HTML (ADR-0054). CommonMark + GFM
 * (tables, strikethrough, task lists, autolinks) via flexmark-java.
 *
 * <p>Raw inline/block HTML in the source (e.g. a historically-authored
 * {@code <img src width>}) passes straight through per the CommonMark spec —
 * this is load-bearing for keeping sized images rendering, and for
 * {@link WikiLinker}'s injected anchors surviving this render step when it
 * runs after link resolution.
 */
public final class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create()));
        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    public String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return markdown;
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }
}
