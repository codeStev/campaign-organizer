package com.campaignorganizer.campaign.application.session.port.published;

import java.util.List;
import java.util.UUID;

/**
 * Published write access for the companion export (ADR-0121). The caller has already established
 * that the campaign belongs to the account; the session must belong to that campaign.
 */
public interface SessionTranscriptWritePort {

    /** Replaces the session's whole transcript. Kinds other than IN_CHARACTER and NARRATION are rejected. */
    SessionTranscriptView replace(UUID campaignId, UUID sessionId, List<LineInput> lines);

    void delete(UUID campaignId, UUID sessionId);

    record LineInput(long offsetMs, String speaker, String kind, String text) {
    }
}
