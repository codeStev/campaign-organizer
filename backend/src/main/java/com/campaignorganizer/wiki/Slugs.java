package com.campaignorganizer.wiki;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

/** URL-safe slug generation (see ADR-0013). */
public final class Slugs {

    private static final int MAX_LENGTH = 200;

    private Slugs() {
    }

    /** Turn arbitrary text into a lowercase, hyphenated, ASCII slug. */
    public static String slugify(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = normalized.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.length() > MAX_LENGTH) {
            slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? "untitled" : slug;
    }

    /**
     * Return the base slug, or the first {@code base-N} that is not already
     * taken according to {@code taken}.
     */
    public static String deduplicate(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        int suffix = 2;
        while (taken.test(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
