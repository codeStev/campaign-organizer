package com.campaignorganizer.ai.domain;

/**
 * The kind of article being drafted, shaping how the draft is written. Mirrors
 * {@code worldbuilding.domain.wiki.ArticleTemplate}'s values but is its own
 * type: {@code contextsOnlyUsePublishedPorts} forbids the {@code ai} context
 * from importing that type directly. See ADR-0075.
 */
public enum ArticleKind {
    GENERIC,
    CHARACTER,
    LOCATION,
    ORGANIZATION,
    SPECIES,
    ITEM,
    EVENT
}
