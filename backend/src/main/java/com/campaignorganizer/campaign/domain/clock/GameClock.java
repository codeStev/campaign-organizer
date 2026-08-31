package com.campaignorganizer.campaign.domain.clock;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A segmented, labeled progress tracker scoped to a campaign (aggregate
 * root, ADR-0084) - Blades in the Dark-style clocks. Always fills up from
 * empty; segments are individually addressable (not strictly sequential) so
 * a specific labeled segment can be marked out of order. Named {@code
 * GameClock} rather than {@code Clock} to avoid colliding with {@code
 * java.time.Clock}, the time port used elsewhere in this service.
 */
public final class GameClock {

    private final UUID id;
    private final UUID campaignId;
    private String title;
    private String description;
    private List<ClockSegment> segments;
    private int position;
    private final Instant createdAt;
    private Instant updatedAt;

    private GameClock(UUID id, UUID campaignId, String title, String description,
                       List<ClockSegment> segments, int position, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(title, description, segments, position);
    }

    public static GameClock create(UUID id, UUID campaignId, String title, String description,
                                    List<ClockSegment> segments, int position, Instant now) {
        return new GameClock(id, campaignId, title, description, segments, position, now, now);
    }

    public static GameClock reconstitute(UUID id, UUID campaignId, String title, String description,
                                          List<ClockSegment> segments, int position, Instant createdAt,
                                          Instant updatedAt) {
        return new GameClock(id, campaignId, title, description, segments, position, createdAt, updatedAt);
    }

    public void update(String title, String description, List<ClockSegment> segments, int position,
                        Instant now) {
        apply(title, description, segments, position);
        this.updatedAt = now;
    }

    private void apply(String title, String description, List<ClockSegment> segments, int position) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Clock title must not be blank");
        }
        if (title.length() > 200) {
            throw new ValidationException("Clock title must not exceed 200 characters");
        }
        this.title = title;
        this.description = description;
        this.segments = segments == null ? List.of() : List.copyOf(segments);
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

    public List<ClockSegment> getSegments() {
        return segments;
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
