package com.campaignorganizer.interchange.companion.application.port.in;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Replaces a session's whole transcript; only story kinds are accepted (ADR-0121). */
public interface ReplaceSessionTranscriptUseCase {

    TranscriptReceipt replace(ReplaceSessionTranscriptCommand command);

    record ReplaceSessionTranscriptCommand(UUID accountId, UUID campaignId, UUID sessionId, List<Line> lines) {
    }

    /** {@code kind} stays text here: the campaign context decides what is allowed. */
    record Line(long offsetMs, String speaker, String kind, String text) {
    }

    record TranscriptReceipt(int lineCount, Instant updatedAt) {
    }
}
