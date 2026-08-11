package com.campaignorganizer.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendars")
public class Calendar {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "days_per_week")
    private Integer daysPerWeek;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Calendar() {
        // for JPA
    }

    public Calendar(UUID worldId, String name, Integer daysPerWeek) {
        this.worldId = worldId;
        this.name = name;
        this.daysPerWeek = daysPerWeek;
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

    public void update(String name, Integer daysPerWeek) {
        this.name = name;
        this.daysPerWeek = daysPerWeek;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getName() {
        return name;
    }

    public Integer getDaysPerWeek() {
        return daysPerWeek;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
