package com.campaignorganizer.campaign.application.session.port.published;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model of a session transcript (ADR-0121). {@code id == null} means none was saved yet. */
public record SessionTranscriptView(UUID id, UUID sessionId, List<LineView> lines, Instant createdAt,
                                    Instant updatedAt) {

    public record LineView(long offsetMs, String speaker, String kind, String text) {
    }
}
