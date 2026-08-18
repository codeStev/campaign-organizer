package com.campaignorganizer.worldbuilding.domain.wiki;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    /** Return {@code body} with wiki-links resolved against {@code index} (keyed by lowercase name). */
    public static String render(String body, Map<String, LinkRef> index) {
        if (body == null || !body.contains("[[")) {
            return body;
        }
        Matcher matcher = LINK.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String label = matcher.group(2);
            LinkRef ref = index.get(target.toLowerCase(Locale.ROOT));
            String text = escape(label != null ? label.trim() : (ref != null ? ref.title() : target));
            String replacement = ref != null
                    ? "<a class=\"wiki-link\" data-article-id=\"" + ref.id() + "\" href=\"#\">" + text + "</a>"
                    : "<span class=\"broken-link\">" + text + "</span>";
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
