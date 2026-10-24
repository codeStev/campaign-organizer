package com.campaignorganizer.interchange.companion.domain.glossary;

import java.util.Optional;

/** What a glossary term names (ADR-0120). */
public enum GlossaryTermKind {
    CHARACTER,
    LOCATION,
    ORGANIZATION,
    SPECIES,
    ITEM,
    CREATURE;

    public String wireValue() {
        return name().toLowerCase();
    }

    /**
     * The kind of an article with the given template, or empty for templates that add noise rather
     * than proper nouns ({@code GENERIC}, {@code EVENT}) and for anything unrecognised.
     */
    public static Optional<GlossaryTermKind> fromArticleTemplate(String template) {
        if (template == null) {
            return Optional.empty();
        }
        return switch (template) {
            case "CHARACTER" -> Optional.of(CHARACTER);
            case "LOCATION" -> Optional.of(LOCATION);
            case "ORGANIZATION" -> Optional.of(ORGANIZATION);
            case "SPECIES" -> Optional.of(SPECIES);
            case "ITEM" -> Optional.of(ITEM);
            default -> Optional.empty();
        };
    }
}
