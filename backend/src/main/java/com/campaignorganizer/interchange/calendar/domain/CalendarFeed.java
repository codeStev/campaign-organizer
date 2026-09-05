package com.campaignorganizer.interchange.calendar.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A campaign's .ics subscription feed — one per campaign, keyed by
 * campaignId rather than its own id (aggregate root, but 1:1 with the
 * campaign it belongs to). See ADR-0108.
 */
public final class CalendarFeed {

    private final UUID campaignId;
    private UUID token;
    private final Instant createdAt;

    private CalendarFeed(UUID campaignId, UUID token, Instant createdAt) {
        this.campaignId = campaignId;
        this.token = token;
        this.createdAt = createdAt;
    }

    public static CalendarFeed create(UUID campaignId, UUID token, Instant now) {
        return new CalendarFeed(campaignId, token, now);
    }

    public static CalendarFeed reconstitute(UUID campaignId, UUID token, Instant createdAt) {
        return new CalendarFeed(campaignId, token, createdAt);
    }

    /** Mints a fresh token, invalidating whatever URL was built from the old one. */
    public void regenerate(UUID newToken) {
        this.token = newToken;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public UUID getToken() {
        return token;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
