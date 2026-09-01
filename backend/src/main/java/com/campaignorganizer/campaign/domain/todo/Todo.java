package com.campaignorganizer.campaign.domain.todo;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A lightweight GM task-list item (aggregate root, ADR-0092) - distinct from
 * a loose thread (retrospective narrative) or a beat (prospective story
 * structure). A null {@code sessionId} means a standing campaign-level todo;
 * a non-null one attaches it to that session, whose date is its implicit
 * due date.
 */
public final class Todo {

    private final UUID id;
    private final UUID campaignId;
    private final UUID sessionId;
    private String text;
    private boolean done;
    private final Instant createdAt;
    private Instant updatedAt;

    private Todo(UUID id, UUID campaignId, UUID sessionId, String text, boolean done,
                Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.sessionId = sessionId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(text, done);
    }

    public static Todo create(UUID id, UUID campaignId, UUID sessionId, String text, boolean done,
                              Instant now) {
        return new Todo(id, campaignId, sessionId, text, done, now, now);
    }

    public static Todo reconstitute(UUID id, UUID campaignId, UUID sessionId, String text, boolean done,
                                    Instant createdAt, Instant updatedAt) {
        return new Todo(id, campaignId, sessionId, text, done, createdAt, updatedAt);
    }

    public void update(String text, boolean done, Instant now) {
        apply(text, done);
        this.updatedAt = now;
    }

    private void apply(String text, boolean done) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Todo text must not be blank");
        }
        if (text.length() > 2000) {
            throw new ValidationException("Todo text must not exceed 2000 characters");
        }
        this.text = text;
        this.done = done;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getText() {
        return text;
    }

    public boolean isDone() {
        return done;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
