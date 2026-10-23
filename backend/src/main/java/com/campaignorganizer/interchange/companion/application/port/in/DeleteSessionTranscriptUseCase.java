package com.campaignorganizer.interchange.companion.application.port.in;

import java.util.UUID;

/** Deletes a session's transcript for the companion export (ADR-0121). */
public interface DeleteSessionTranscriptUseCase {

    void delete(UUID accountId, UUID campaignId, UUID sessionId);
}
