package com.campaignorganizer.worldbuilding.domain.wiki;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code [[target]]} and {@code [[target|label]]} wiki-links in an
 * article body into anchors (or broken-link spans). See ADR-0014.
 *
 * <p>Pure domain logic: link resolution against a supplied index; fetching the
 * index from the datastore is the caller's (application) concern.
 */
public final class WikiLinker {

    private static final Pattern LINK =
            Pattern.compile("\\[\\[\\s*([^\\]|]+?)\\s*(?:\\|\\s*([^\\]]+?)\\s*)?\\]\\]");

    private WikiLinker() {
    }

    /** An article an anchor can point at. */
    public record LinkRef(UUID id, String title) {
    }

    /** Lowercased {@code [[target]]} names referenced in a body (before any {@code |}). */
    public static Set<String> linkTargets(String body) {
        Set<String> targets = new HashSet<>();
        if (body == null || !body.contains("[[")) {
            return targets;
        }
        Matcher matcher = LINK.matcher(body);
        while (matcher.find()) {
            targets.add(matcher.group(1).trim().toLowerCase(Locale.ROOT));
        }
        return targets;
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
        if (body == null || !body.contains("[[")) {
            return body;
        }
        Matcher matcher = LINK.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String label = matcher.group(2);
            LinkRef ref = index.get(target.toLowerCase(Locale.ROOT));
            String rawText = label != null ? label.trim() : (ref != null ? ref.title() : target);
            String text = escape ? escape(rawText) : rawText;
            String replacement = ref != null ? onResolved.apply(ref, text) : onBroken.apply(text);
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
