package com.campaignorganizer.worldbuilding.domain.wiki;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds plain-text mentions of other articles' known names (title or an
 * alias, ADR-0116) inside a body that aren't already linked, so they can be
 * reviewed and converted to real {@code [[link]]}s — a manual, reviewable
 * complement to typing links by hand, useful for articles written before
 * this app's linking system existed.
 *
 * <p>Pure domain logic, same style as {@link WikiLinker}: matching against
 * a supplied candidate list; fetching that list from the datastore is the
 * caller's (application) concern.
 */
public final class AutolinkScanner {

    /** Names shorter than this are skipped - too noisy to be useful matches. */
    private static final int MIN_NAME_LENGTH = 3;

    private static final Pattern CODE_FENCE = Pattern.compile("```[\\s\\S]*?```");
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`\\n]*`");

    private AutolinkScanner() {
    }

    /** A linkable article and its name-forms, canonical title first, then aliases. */
    public record Candidate(UUID articleId, List<String> names) {
    }

    /** One reviewable match: which article it could link to, every name-form that
     * article is known by (for a "use this alias instead" picker), the exact text
     * found, its ordinal among matches of the same target in this body, and a
     * short surrounding snippet. */
    public record Occurrence(UUID targetArticleId, List<String> candidateNames, String matchedText,
                             int occurrenceIndex, String snippet, int start, int end) {
    }

    /** A reviewed decision: convert the {@code occurrenceIndex}-th match of
     * {@code targetArticleId} in this body, writing {@code chosenName} (the
     * target's title or one of its aliases) as the link's target text. */
    public record Selection(UUID targetArticleId, int occurrenceIndex, String chosenName) {
    }

    public static List<Occurrence> scan(String body, UUID selfArticleId, List<Candidate> candidates) {
        if (body == null || body.isEmpty()) {
            return List.of();
        }
        List<int[]> excluded = excludedSpans(body);
        List<RawMatch> raw = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (candidate.articleId().equals(selfArticleId)) {
                continue;
            }
            for (String name : candidate.names()) {
                String trimmed = name == null ? "" : name.trim();
                if (trimmed.length() < MIN_NAME_LENGTH) {
                    continue;
                }
                Pattern pattern = Pattern.compile("\\b" + Pattern.quote(trimmed) + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(body);
                while (matcher.find()) {
                    if (overlapsAny(excluded, matcher.start(), matcher.end())) {
                        continue;
                    }
                    raw.add(new RawMatch(candidate.articleId(), candidate.names(), matcher.start(), matcher.end(),
                            body.substring(matcher.start(), matcher.end())));
                }
            }
        }
        // Longest match wins at a given position (e.g. "Waterdeep" inside "North
        // Waterdeep Docks"); ties broken by candidate order, deterministic given a
        // stable sort.
        raw.sort(Comparator.<RawMatch>comparingInt(r -> r.start).thenComparingInt(r -> -(r.end - r.start)));
        List<RawMatch> accepted = new ArrayList<>();
        int lastEnd = -1;
        for (RawMatch r : raw) {
            if (r.start >= lastEnd) {
                accepted.add(r);
                lastEnd = r.end;
            }
        }
        Map<UUID, Integer> counters = new HashMap<>();
        List<Occurrence> result = new ArrayList<>();
        for (RawMatch r : accepted) {
            int occurrenceIndex = counters.merge(r.targetArticleId, 1, Integer::sum) - 1;
            result.add(new Occurrence(r.targetArticleId, r.names, r.matchedText, occurrenceIndex,
                    snippet(body, r.start, r.end), r.start, r.end));
        }
        return result;
    }

    /** Re-runs {@link #scan} against the current body and replaces only the selected
     * occurrences with {@code [[chosenName|original matched text]]} - always the
     * labeled form, since an unlabeled link displays the target's canonical title on
     * render (see {@code WikiLinker}), not whatever text sits inside the brackets;
     * only the labeled form actually preserves the author's original wording. */
    public static String apply(String body, UUID selfArticleId, List<Candidate> candidates,
                               Set<Selection> selected) {
        if (body == null || body.isEmpty() || selected.isEmpty()) {
            return body;
        }
        Map<SelectionKey, String> chosenByKey = new HashMap<>();
        for (Selection s : selected) {
            chosenByKey.put(new SelectionKey(s.targetArticleId(), s.occurrenceIndex()), s.chosenName());
        }
        List<Occurrence> toApply = new ArrayList<>();
        for (Occurrence o : scan(body, selfArticleId, candidates)) {
            if (chosenByKey.containsKey(new SelectionKey(o.targetArticleId(), o.occurrenceIndex()))) {
                toApply.add(o);
            }
        }
        if (toApply.isEmpty()) {
            return body;
        }
        toApply.sort(Comparator.comparingInt(Occurrence::start));
        StringBuilder out = new StringBuilder();
        int cursor = 0;
        for (Occurrence o : toApply) {
            out.append(body, cursor, o.start());
            String chosenName = chosenByKey.get(new SelectionKey(o.targetArticleId(), o.occurrenceIndex()));
            out.append("[[").append(chosenName).append('|').append(o.matchedText()).append("]]");
            cursor = o.end();
        }
        out.append(body, cursor, body.length());
        return out.toString();
    }

    private static List<int[]> excludedSpans(String body) {
        List<int[]> spans = new ArrayList<>();
        for (int[] span : WikiLinker.linkedSpans(body)) {
            spans.add(span);
        }
        addMatches(spans, CODE_FENCE, body);
        addMatches(spans, INLINE_CODE, body);
        return spans;
    }

    private static void addMatches(List<int[]> spans, Pattern pattern, String body) {
        Matcher matcher = pattern.matcher(body);
        while (matcher.find()) {
            spans.add(new int[] {matcher.start(), matcher.end()});
        }
    }

    private static boolean overlapsAny(List<int[]> spans, int start, int end) {
        for (int[] span : spans) {
            if (start < span[1] && end > span[0]) {
                return true;
            }
        }
        return false;
    }

    private static final int SNIPPET_CONTEXT = 30;

    private static String snippet(String body, int start, int end) {
        int from = Math.max(0, start - SNIPPET_CONTEXT);
        int to = Math.min(body.length(), end + SNIPPET_CONTEXT);
        String prefix = from > 0 ? "…" : "";
        String suffix = to < body.length() ? "…" : "";
        return prefix + body.substring(from, to).replace('\n', ' ') + suffix;
    }

    private record RawMatch(UUID targetArticleId, List<String> names, int start, int end, String matchedText) {
    }

    private record SelectionKey(UUID targetArticleId, int occurrenceIndex) {
    }
}
