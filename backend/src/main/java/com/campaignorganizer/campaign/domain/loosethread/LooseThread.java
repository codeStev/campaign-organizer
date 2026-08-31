package com.campaignorganizer.campaign.domain.loosethread;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A retrospective note on something improvised at the table that players
 * latched onto (aggregate root, ADR-0085) - the opposite of a beat, which is
 * prospective. Scoped to one session; carries a denormalized
 * {@code campaignId} so a future dashboard can query "open threads for
 * campaign X" without walking every session.
 */
public final class LooseThread {

    private final UUID id;
    private final UUID sessionId;
    private final UUID campaignId;
    private String text;
    private LooseThreadStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private LooseThread(UUID id, UUID sessionId, UUID campaignId, String text, LooseThreadStatus status,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.campaignId = campaignId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(text, status);
    }

    public static LooseThread create(UUID id, UUID sessionId, UUID campaignId, String text,
                                     LooseThreadStatus status, Instant now) {
        return new LooseThread(id, sessionId, campaignId, text, status, now, now);
    }

    public static LooseThread reconstitute(UUID id, UUID sessionId, UUID campaignId, String text,
                                           LooseThreadStatus status, Instant createdAt, Instant updatedAt) {
        return new LooseThread(id, sessionId, campaignId, text, status, createdAt, updatedAt);
    }

    public void update(String text, LooseThreadStatus status, Instant now) {
        apply(text, status);
        this.updatedAt = now;
    }

    private void apply(String text, LooseThreadStatus status) {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Loose thread text must not be blank");
        }
        if (text.length() > 2000) {
            throw new ValidationException("Loose thread text must not exceed 2000 characters");
        }
        this.text = text;
        this.status = status == null ? LooseThreadStatus.OPEN : status;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public String getText() {
        return text;
    }

    public LooseThreadStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
