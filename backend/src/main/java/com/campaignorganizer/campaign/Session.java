package com.campaignorganizer.campaign;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    private UUID id;

    @Column(name = "campaign_id", nullable = false, updatable = false)
    private UUID campaignId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "session_number")
    private Integer sessionNumber;

    @Column(name = "date")
    private LocalDate date;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Session() {
        // for JPA
    }

    public Session(UUID campaignId, String title, Integer sessionNumber, LocalDate date,
                   String summary, String notes) {
        this.campaignId = campaignId;
        this.title = title;
        this.sessionNumber = sessionNumber;
        this.date = date;
        this.summary = summary;
        this.notes = notes;
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

    public void update(String title, Integer sessionNumber, LocalDate date, String summary, String notes) {
        this.title = title;
        this.sessionNumber = sessionNumber;
        this.date = date;
        this.summary = summary;
        this.notes = notes;
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

    public Integer getSessionNumber() {
        return sessionNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getSummary() {
        return summary;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
