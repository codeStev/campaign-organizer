package com.campaignorganizer.worldbuilding.domain.timeline;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A dated event on a timeline (aggregate root). The domain enforces a non-blank
 * title; the month/day-fits-calendar rule is applied by the application (it needs
 * the timeline's calendar).
 */
public final class TimelineEvent {

    private final UUID id;
    private final UUID timelineId;
    private UUID articleId;
    private String title;
    private String description;
    private int year;
    private Integer month;
    private Integer day;
    private final Instant createdAt;
    private Instant updatedAt;

    private TimelineEvent(UUID id, UUID timelineId, UUID articleId, String title, String description,
                          int year, Integer month, Integer day, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.timelineId = timelineId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(articleId, title, description, year, month, day);
    }

    public static TimelineEvent create(UUID id, UUID timelineId, UUID articleId, String title,
                                       String description, int year, Integer month, Integer day,
                                       Instant now) {
        return new TimelineEvent(id, timelineId, articleId, title, description, year, month, day, now, now);
    }

    public static TimelineEvent reconstitute(UUID id, UUID timelineId, UUID articleId, String title,
                                             String description, int year, Integer month, Integer day,
                                             Instant createdAt, Instant updatedAt) {
        return new TimelineEvent(id, timelineId, articleId, title, description, year, month, day,
                createdAt, updatedAt);
    }

    public void update(UUID articleId, String title, String description, int year, Integer month,
                       Integer day, Instant now) {
        apply(articleId, title, description, year, month, day);
        this.updatedAt = now;
    }

    private void apply(UUID articleId, String title, String description, int year, Integer month,
                       Integer day) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Event title must not be blank");
        }
        this.articleId = articleId;
        this.title = title;
        this.description = description;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    public UUID getId() {
        return id;
    }

    public UUID getTimelineId() {
        return timelineId;
    }

    public UUID getArticleId() {
        return articleId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getYear() {
        return year;
    }

    public Integer getMonth() {
        return month;
    }

    public Integer getDay() {
        return day;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
