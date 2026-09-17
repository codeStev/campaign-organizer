package com.campaignorganizer.interchange.foundry.domain;

import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves {@code [[target]]}/{@code [[target|label]]} wiki-links in a Session Guide's
 * beat text into real, clickable Foundry content links instead of the plain Markdown
 * emphasis every other pushed document uses (ADR-0115) — {@code @UUID[JournalEntry.<id>]
 * {label}} is Foundry's own built-in content-link enricher syntax (the same reference
 * style already used for {@code TableResult.documentUuid} in the roll-table push, just
 * embedded as literal text in a body instead of a structured field). This is a
 * well-known, stable Foundry core feature but has NOT been independently confirmed
 * against Foundry's official docs the way other assumptions in this feature were —
 * flag as unverified until tested against a live relay+Foundry instance, same as this
 * feature's other flagged assumptions.
 *
 * <p>Pure domain logic, framework-free — mirrors {@code MediaEmbedRewriter}'s shape.
 * Has its own copy of the wiki-link grammar's regex, since {@code interchange/foundry}
 * cannot import {@code worldbuilding.domain.wiki.WikiLinker} (a different bounded
 * context's domain ring, not published) — this exact regex is already independently
 * duplicated between {@code WikiLinker} and the frontend's {@code wikiLinkExtension.ts}
 * per that file's own comment, so a third small domain-local copy here is consistent
 * with existing precedent, not a new anti-pattern.
 */
public final class FoundryDocumentLinkRewriter {

    private static final Pattern LINK =
            Pattern.compile("\\[\\[\\s*([^\\]|]+?)\\s*(?:\\|\\s*([^\\]]+?)\\s*)?\\]\\]");

    private FoundryDocumentLinkRewriter() {
    }

    /** Replace each {@code [[target]]}/{@code [[target|label]]} in {@code body}: resolved
     * (via {@code resolver}, keyed by lowercased target name) becomes {@code @UUID[JournalEntry.
     * <stableIdFor(id)>]{label}}; unresolved becomes plain {@code *label*} italic — matching
     * {@code WikiLinker.renderMarkdown}'s existing broken-link fallback for visual consistency
     * with how every other pushed document already shows broken links. */
    public static String rewrite(String body, Function<String, UUID> resolver, Function<UUID, String> stableIdFor) {
        if (body == null || !body.contains("[[")) {
            return body;
        }
        Matcher matcher = LINK.matcher(body);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String target = matcher.group(1).trim();
            String labelGroup = matcher.group(2);
            String label = labelGroup != null ? labelGroup.trim() : target;
            UUID resolvedId = resolver.apply(target.toLowerCase(java.util.Locale.ROOT));
            String replacement = resolvedId != null
                    ? "@UUID[JournalEntry." + stableIdFor.apply(resolvedId) + "]{" + label + "}"
                    : "*" + label + "*";
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
