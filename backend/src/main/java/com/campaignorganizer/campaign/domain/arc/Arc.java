package com.campaignorganizer.campaign.domain.arc;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/** A story arc within a campaign, ordering a run of beats (aggregate root). */
public final class Arc {

    private final UUID id;
    private final UUID campaignId;
    private String title;
    private String description;
    private ArcStatus status;
    private int position;
    private final Instant createdAt;
    private Instant updatedAt;

    private Arc(UUID id, UUID campaignId, String title, String description, ArcStatus status,
                int position, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(title, description, status, position);
    }

    public static Arc create(UUID id, UUID campaignId, String title, String description,
                             ArcStatus status, int position, Instant now) {
        return new Arc(id, campaignId, title, description, status, position, now, now);
    }

    public static Arc reconstitute(UUID id, UUID campaignId, String title, String description,
                                   ArcStatus status, int position, Instant createdAt, Instant updatedAt) {
        return new Arc(id, campaignId, title, description, status, position, createdAt, updatedAt);
    }

    public void update(String title, String description, ArcStatus status, int position, Instant now) {
        apply(title, description, status, position);
        this.updatedAt = now;
    }

    private void apply(String title, String description, ArcStatus status, int position) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Arc title must not be blank");
        }
        this.title = title;
        this.description = description;
        this.status = status == null ? ArcStatus.PLANNED : status;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public ArcStatus getStatus() {
        return status;
    }

    public int getPosition() {
        return position;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
