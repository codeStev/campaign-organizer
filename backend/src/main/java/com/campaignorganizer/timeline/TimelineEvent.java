package com.campaignorganizer.timeline;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "timeline_events")
public class TimelineEvent {

    @Id
    private UUID id;

    @Column(name = "timeline_id", nullable = false, updatable = false)
    private UUID timelineId;

    @Column(name = "article_id")
    private UUID articleId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "day")
    private Integer day;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected TimelineEvent() {
        // for JPA
    }

    public TimelineEvent(UUID timelineId, UUID articleId, String title, String description,
                         int year, Integer month, Integer day) {
        this.timelineId = timelineId;
        this.articleId = articleId;
        this.title = title;
        this.description = description;
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void update(UUID articleId, String title, String description,
                       int year, Integer month, Integer day) {
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
