package com.campaignorganizer.interchange.companion.domain.glossary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects candidate terms and yields the final glossary (ADR-0120): blank names are dropped,
 * terms with the same name and kind (case-insensitive) are merged with their aliases combined, an
 * alias equal to the term's own name is dropped, and the result is sorted by name.
 */
public final class GlossaryBuilder {

    private record Key(String lowerName, GlossaryTermKind kind) {
    }

    private final Map<Key, Entry> entries = new LinkedHashMap<>();

    public GlossaryBuilder add(String name, GlossaryTermKind kind, List<String> aliases) {
        if (name == null || name.isBlank()) {
            return this;
        }
        String trimmed = name.trim();
        Entry entry = entries.computeIfAbsent(new Key(trimmed.toLowerCase(), kind), key -> new Entry(trimmed));
        if (aliases != null) {
            aliases.forEach(entry::addAlias);
        }
        return this;
    }

    public List<GlossaryTerm> build() {
        return entries.entrySet().stream()
                .map(e -> new GlossaryTerm(e.getValue().name, e.getKey().kind(), e.getValue().sortedAliases()))
                .sorted(Comparator.comparing((GlossaryTerm t) -> t.name().toLowerCase()).thenComparing(GlossaryTerm::kind))
                .toList();
    }

    private static final class Entry {
        private final String name;
        private final Map<String, String> aliasesByLowercase = new LinkedHashMap<>();

        private Entry(String name) {
            this.name = name;
        }

        private void addAlias(String alias) {
            if (alias == null || alias.isBlank()) {
                return;
            }
            String trimmed = alias.trim();
            if (!trimmed.equalsIgnoreCase(name)) {
                aliasesByLowercase.putIfAbsent(trimmed.toLowerCase(), trimmed);
            }
        }

        private List<String> sortedAliases() {
            List<String> sorted = new ArrayList<>(aliasesByLowercase.values());
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            return List.copyOf(sorted);
        }
    }
}
