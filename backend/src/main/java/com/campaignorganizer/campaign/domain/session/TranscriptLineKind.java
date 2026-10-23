package com.campaignorganizer.campaign.domain.session;

import com.campaignorganizer.shared.domain.ValidationException;

/**
 * What a transcript line is. Deliberately only story content (ADR-0121): meta talk, off-topic talk
 * and unclear lines have no representation here, so the organizer cannot hold them.
 */
public enum TranscriptLineKind {
    IN_CHARACTER,
    NARRATION;

    public static TranscriptLineKind parse(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ValidationException("Unknown transcript line kind: " + value);
        }
    }
}
