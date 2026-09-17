package com.campaignorganizer.interchange.foundry.domain;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts and rewrites this app's own {@code /api/media/{id}/content} embeds
 * inside a Markdown body (ADR-0115), so referenced images can be uploaded to
 * Foundry and the body updated to point at the resulting Foundry-side path.
 * Pure domain logic, framework-free — mirrors {@code interchange.export}'s
 * {@code ImportService} media-URL regex and rewrite convention.
 */
public final class MediaEmbedRewriter {

    private static final Pattern MEDIA_URL = Pattern.compile("/api/media/([0-9a-fA-F-]{36})/content");

    private MediaEmbedRewriter() {
    }

    /** Every distinct media id referenced in {@code body}, in first-seen order. */
    public static Set<UUID> mediaIdsIn(String body) {
        Set<UUID> ids = new LinkedHashSet<>();
        if (body == null) {
            return ids;
        }
        Matcher matcher = MEDIA_URL.matcher(body);
        while (matcher.find()) {
            ids.add(UUID.fromString(matcher.group(1)));
        }
        return ids;
    }

    /** Replace each {@code /api/media/{id}/content} reference with whatever {@code resolver}
     * returns for that id, or leave it untouched if {@code resolver} returns {@code null}. */
    public static String rewrite(String body, Function<UUID, String> resolver) {
        if (body == null) {
            return null;
        }
        Matcher matcher = MEDIA_URL.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            UUID mediaId = UUID.fromString(matcher.group(1));
            String replacement = resolver.apply(mediaId);
            matcher.appendReplacement(out,
                    Matcher.quoteReplacement(replacement != null ? replacement : matcher.group(0)));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
