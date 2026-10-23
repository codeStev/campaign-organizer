package com.campaignorganizer.campaign.domain.session;

import com.campaignorganizer.shared.domain.ValidationException;

/**
 * One line of a session transcript. {@code speaker} is the name of the character being voiced (or
 * null, e.g. for narration); there is no place for a player's name (ADR-0121).
 */
public record TranscriptLine(long offsetMs, String speaker, TranscriptLineKind kind, String text) {

    public static final int MAX_TEXT_LENGTH = 2000;
    public static final int MAX_SPEAKER_LENGTH = 100;

    public TranscriptLine {
        if (offsetMs < 0) {
            throw new ValidationException("Transcript line offset must not be negative");
        }
        if (kind == null) {
            throw new ValidationException("Transcript line needs a kind");
        }
        if (text == null || text.isBlank()) {
            throw new ValidationException("Transcript line text must not be blank");
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Transcript line text must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        speaker = speaker == null || speaker.isBlank() ? null : speaker.trim();
        if (speaker != null && speaker.length() > MAX_SPEAKER_LENGTH) {
            throw new ValidationException("Transcript speaker must be at most " + MAX_SPEAKER_LENGTH + " characters");
        }
    }
}
