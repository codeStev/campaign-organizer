package com.campaignorganizer.campaign.domain.session;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The transcript of one session (aggregate root, ADR-0121): the story lines of a recorded
 * session, in order. One per session; always replaced as a whole.
 */
public final class SessionTranscript {

    public static final int MAX_LINES = 20_000;

    private final UUID id;
    private final UUID sessionId;
    private List<TranscriptLine> lines;
    private final Instant createdAt;
    private Instant updatedAt;

    private SessionTranscript(UUID id, UUID sessionId, List<TranscriptLine> lines, Instant createdAt,
                              Instant updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(lines);
    }

    public static SessionTranscript create(UUID id, UUID sessionId, List<TranscriptLine> lines, Instant now) {
        return new SessionTranscript(id, sessionId, lines, now, now);
    }

    public static SessionTranscript reconstitute(UUID id, UUID sessionId, List<TranscriptLine> lines,
                                                 Instant createdAt, Instant updatedAt) {
        return new SessionTranscript(id, sessionId, lines, createdAt, updatedAt);
    }

    public void replaceLines(List<TranscriptLine> lines, Instant now) {
        apply(lines);
        this.updatedAt = now;
    }

    private void apply(List<TranscriptLine> lines) {
        if (lines == null || lines.isEmpty()) {
            throw new ValidationException("A transcript needs at least one line; delete it instead");
        }
        if (lines.size() > MAX_LINES) {
            throw new ValidationException("A transcript has at most " + MAX_LINES + " lines");
        }
        this.lines = List.copyOf(lines);
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public List<TranscriptLine> getLines() {
        return lines;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
