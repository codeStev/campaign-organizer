package com.campaignorganizer.worldbuilding.domain.wiki;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code [[target]]}/{@code [[target|label]]} wiki-links (ADR-0014)
 * and {@code [label](target)} Markdown-standard links (ADR-0116) in an
 * article body into anchors (or broken-link spans) — the second form is
 * accepted only when {@code target} doesn't look like a URL (no scheme, no
 * leading {@code /} or {@code #}), so a real hyperlink is left completely
 * untouched for the Markdown renderer to handle as it always has.
 *
 * <p>Pure domain logic: link resolution against a supplied index; fetching the
 * index from the datastore is the caller's (application) concern.
 */
public final class WikiLinker {

    private static final Pattern LINK = Pattern.compile(
            "\\[\\[\\s*([^\\]|]+?)\\s*(?:\\|\\s*([^\\]]+?)\\s*)?\\]\\]"
                    + "|(?<!!)\\[([^\\]]+?)\\]\\(([^)]+?)\\)");

    private static final Pattern URL_LIKE_TARGET = Pattern.compile("^(?:[a-zA-Z][a-zA-Z0-9+.-]*:|/|#)");

    private WikiLinker() {
    }

    /** An article an anchor can point at. */
    public record LinkRef(UUID id, String title) {
    }

    /** Lowercased link target names referenced in a body — both syntaxes, excluding
     * {@code [label](url)} matches whose target looks like a real URL. */
    public static Set<String> linkTargets(String body) {
        Set<String> targets = new HashSet<>();
        if (body == null || body.indexOf('[') < 0) {
            return targets;
        }
        Matcher matcher = LINK.matcher(body);
        while (matcher.find()) {
            boolean wikiForm = matcher.group(1) != null;
            String target = (wikiForm ? matcher.group(1) : matcher.group(4)).trim();
            if (!wikiForm && looksLikeUrl(target)) {
                continue;
            }
            targets.add(target.toLowerCase(Locale.ROOT));
        }
        return targets;
    }

    /** Character spans (start inclusive, end exclusive) already covered by a link of either
     * syntax, regardless of whether it resolves — used by {@code AutolinkScanner} to avoid
     * re-suggesting text that's already linked. */
    public static List<int[]> linkedSpans(String body) {
        List<int[]> spans = new ArrayList<>();
        if (body == null || body.indexOf('[') < 0) {
            return spans;
        }
        Matcher matcher = LINK.matcher(body);
        while (matcher.find()) {
            spans.add(new int[] {matcher.start(), matcher.end()});
        }
        return spans;
    }

    /** Return {@code body} with wiki-links resolved against {@code index} (keyed by lowercase name)
     * into anchors (or broken-link spans). */
    public static String render(String body, Map<String, LinkRef> index) {
        return renderWith(body, index,
                (ref, text) -> "<a class=\"wiki-link\" data-article-id=\"" + ref.id() + "\" href=\"#\">"
                        + text + "</a>",
                text -> "<span class=\"broken-link\">" + text + "</span>",
                true);
    }

    /** Return {@code body} with wiki-links resolved against {@code index} into plain Markdown
     * emphasis instead of HTML — resolved links become {@code **label**}, broken links become
     * {@code *label*} (ADR-0115: for pushing a body to an external, non-HTML consumer such as a
     * Foundry VTT Markdown journal page, where the HTML anchors {@link #render} produces are
     * meaningless — they carry a {@code data-article-id} and {@code href="#"} that only this
     * app's own SPA resolves). Never HTML-escaped, since the output is Markdown, not HTML. */
    public static String renderMarkdown(String body, Map<String, LinkRef> index) {
        return renderWith(body, index,
                (ref, text) -> "**" + text + "**",
                text -> "*" + text + "*",
                false);
    }

    private static String renderWith(String body, Map<String, LinkRef> index,
                                     BiFunction<LinkRef, String, String> onResolved,
                                     Function<String, String> onBroken, boolean escape) {
        if (body == null || body.indexOf('[') < 0) {
            return body;
        }
        Matcher matcher = LINK.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            boolean wikiForm = matcher.group(1) != null;
            String target = (wikiForm ? matcher.group(1) : matcher.group(4)).trim();
            if (!wikiForm && looksLikeUrl(target)) {
                // A real hyperlink, not an article reference - leave it untouched for
                // the Markdown renderer to handle exactly as it always has.
                matcher.appendReplacement(out, Matcher.quoteReplacement(matcher.group(0)));
                continue;
            }
            String label = wikiForm ? matcher.group(2) : matcher.group(3);
            LinkRef ref = index.get(target.toLowerCase(Locale.ROOT));
            String rawText = label != null ? label.trim() : (ref != null ? ref.title() : target);
            String text = escape ? escape(rawText) : rawText;
            String replacement = ref != null ? onResolved.apply(ref, text) : onBroken.apply(text);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static boolean looksLikeUrl(String target) {
        return URL_LIKE_TARGET.matcher(target).find();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
