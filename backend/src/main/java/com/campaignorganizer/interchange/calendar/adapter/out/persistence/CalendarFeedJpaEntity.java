package com.campaignorganizer.interchange.calendar.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Persistence model for a campaign's calendar feed (maps the {@code campaign_calendar_feeds} table). */
@Entity
@Table(name = "campaign_calendar_feeds")
public class CalendarFeedJpaEntity {

    @Id
    @Column(name = "campaign_id")
    private UUID campaignId;

    @Column(nullable = false, unique = true)
    private UUID token;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected CalendarFeedJpaEntity() {
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getToken() {
        return token;
    }

    public void setToken(UUID token) {
        this.token = token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
