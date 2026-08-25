package com.campaignorganizer.ai.domain;

import com.campaignorganizer.shared.domain.ValidationException;

/**
 * What the owner asked to have drafted (aggregate root's only real invariant:
 * bounded, non-blank input). Pure domain — no framework. See ADR-0064.
 */
public record DraftInstructions(String instructions, String existingContent) {

    private static final int MAX_INSTRUCTIONS = 2000;
    private static final int MAX_EXISTING_CONTENT = 20000;

    public DraftInstructions {
        if (instructions == null || instructions.isBlank()) {
            throw new ValidationException("Instructions must not be blank");
        }
        if (instructions.length() > MAX_INSTRUCTIONS) {
            throw new ValidationException("Instructions exceed " + MAX_INSTRUCTIONS + " characters");
        }
        if (existingContent != null && existingContent.length() > MAX_EXISTING_CONTENT) {
            throw new ValidationException("Existing content exceeds " + MAX_EXISTING_CONTENT + " characters");
        }
        existingContent = existingContent == null ? "" : existingContent;
    }
}
