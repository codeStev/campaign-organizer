package com.campaignorganizer.ai.domain;

import com.campaignorganizer.shared.domain.ValidationException;

/**
 * The GM's private session notes to condense into an AI summary (aggregate
 * root's only real invariant: bounded, non-blank input). Pure domain — no
 * framework. See ADR-0082.
 */
public record SessionNotesToSummarize(String notes) {

    private static final int MAX_NOTES = 20000;

    public SessionNotesToSummarize {
        if (notes == null || notes.isBlank()) {
            throw new ValidationException("Notes must not be blank");
        }
        if (notes.length() > MAX_NOTES) {
            throw new ValidationException("Notes exceed " + MAX_NOTES + " characters");
        }
    }
}
