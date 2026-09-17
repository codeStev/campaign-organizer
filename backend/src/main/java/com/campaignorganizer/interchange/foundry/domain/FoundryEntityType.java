package com.campaignorganizer.interchange.foundry.domain;

/** The Campaign Organizer entity kinds that can be pushed to Foundry (ADR-0115). */
public enum FoundryEntityType {
    ARTICLE,
    HANDOUT,
    ROLL_TABLE,
    CARD_DECK,
    SESSION_GUIDE,
    /** A whole wiki category pushed in one action (either as a Foundry folder of separate
     * JournalEntry documents, or as a single JournalEntry with one page per article) — the
     * push record tracks the category itself, distinct from the per-article records each
     * contained article also gets. */
    CATEGORY,
    /** The entire world's wiki pushed in one action — same FOLDER/SINGLE_DOCUMENT choice as
     * {@link #CATEGORY}, just scoped to every category and article in the world instead of
     * one category. Tracked with {@code entityId = worldId}. */
    WIKI
}
