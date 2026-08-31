package com.campaignorganizer.tagging.domain;

/**
 * Kinds of entity that can carry tags (FR-47). v1 covers articles and
 * statblocks only; a later entity type is an additive enum value plus one
 * more existence adapter — the store itself does not change shape.
 */
public enum EntityType {
    ARTICLE, STATBLOCK
}
