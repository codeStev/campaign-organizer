package com.campaignorganizer.interchange.companion.application.port.in;

import java.time.Instant;
import java.util.UUID;

/** Writes an exported summary without overwriting one the caller did not write (ADR-0121). */
public interface WriteSessionSummaryUseCase {

    CompanionSession write(WriteSessionSummaryCommand command);

    record WriteSessionSummaryCommand(UUID accountId, UUID campaignId, UUID sessionId, String summary,
                                      Instant expectedUpdatedAt) {
    }
}
