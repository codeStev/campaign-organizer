package com.campaignorganizer.campaign.adapter.session.in.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response payloads for session transcripts (mirrors docs/api/openapi.yaml). */
public final class SessionTranscriptWebDtos {

    private SessionTranscriptWebDtos() {
    }

    public record TranscriptLineResponse(long offsetMs, String speaker, String kind, String text) {
    }

    public record SessionTranscriptResponse(UUID id, UUID sessionId, List<TranscriptLineResponse> lines,
                                            Instant createdAt, Instant updatedAt) {
    }
}
