package com.campaignorganizer.ai.domain;

/** How much (and what kind of) text to generate, from a short teaser up to a
 * full first draft. See ADR-0075. */
public enum DraftLevel {
    QUICK_INSPIRATION,
    READ_ALOUD,
    BASIC_INFO,
    FULL_DRAFT
}
