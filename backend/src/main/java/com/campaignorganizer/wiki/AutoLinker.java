package com.campaignorganizer.wiki;

import com.campaignorganizer.wiki.ArticleRepository.ArticleRef;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Resolves {@code [[target]]} and {@code [[target|label]]} wiki-links in an
 * article body into anchors (or broken-link spans). See ADR-0014.
 */
@Component
public class AutoLinker {

    private static final Pattern LINK =
            Pattern.compile("\\[\\[\\s*([^\\]|]+?)\\s*(?:\\|\\s*([^\\]]+?)\\s*)?\\]\\]");

    private final ArticleRepository articles;

    public AutoLinker(ArticleRepository articles) {
        this.articles = articles;
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

    /** Return {@code body} with wiki-links resolved against the world's articles. */
    public String render(UUID worldId, String body) {
        if (body == null || !body.contains("[[")) {
            return body;
        }
        Map<String, ArticleRef> index = indexArticles(worldId);
        Matcher matcher = LINK.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String label = matcher.group(2);
            ArticleRef ref = index.get(target.toLowerCase(Locale.ROOT));
            String text = escape(label != null ? label.trim() : (ref != null ? ref.getTitle() : target));
            String replacement = ref != null
                    ? "<a class=\"wiki-link\" data-article-id=\"" + ref.getId() + "\" href=\"#\">" + text + "</a>"
                    : "<span class=\"broken-link\">" + text + "</span>";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private Map<String, ArticleRef> indexArticles(UUID worldId) {
        List<ArticleRef> refs = articles.findRefsByWorldId(worldId);
        Map<String, ArticleRef> index = new HashMap<>();
        for (ArticleRef ref : refs) {
            // Title takes precedence over slug on collision; slug fills gaps.
            index.putIfAbsent(ref.getSlug().toLowerCase(Locale.ROOT), ref);
        }
        for (ArticleRef ref : refs) {
            index.put(ref.getTitle().toLowerCase(Locale.ROOT), ref);
        }
        return index;
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
