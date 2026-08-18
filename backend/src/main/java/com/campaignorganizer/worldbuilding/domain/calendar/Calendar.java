package com.campaignorganizer.worldbuilding.domain.calendar;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A custom (fantasy) calendar with an ordered list of months (aggregate root).
 * Pure domain: enforces a non-blank name, a positive week length, and at least one
 * month; months are value objects.
 */
public final class Calendar {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private Integer daysPerWeek;
    private List<Month> months;
    private final Instant createdAt;
    private Instant updatedAt;

    private Calendar(UUID id, UUID worldId, String name, Integer daysPerWeek, List<Month> months,
                     Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, daysPerWeek, months);
    }

    public static Calendar create(UUID id, UUID worldId, String name, Integer daysPerWeek,
                                  List<Month> months, Instant now) {
        return new Calendar(id, worldId, name, daysPerWeek, months, now, now);
    }

    public static Calendar reconstitute(UUID id, UUID worldId, String name, Integer daysPerWeek,
                                        List<Month> months, Instant createdAt, Instant updatedAt) {
        return new Calendar(id, worldId, name, daysPerWeek, months, createdAt, updatedAt);
    }

    public void update(String name, Integer daysPerWeek, List<Month> months, Instant now) {
        apply(name, daysPerWeek, months);
        this.updatedAt = now;
    }

    private void apply(String name, Integer daysPerWeek, List<Month> months) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Calendar name must not be blank");
        }
        if (daysPerWeek != null && daysPerWeek <= 0) {
            throw new ValidationException("Days per week must be positive");
        }
        if (months == null || months.isEmpty()) {
            throw new ValidationException("A calendar needs at least one month");
        }
        this.name = name;
        this.daysPerWeek = daysPerWeek;
        this.months = List.copyOf(months);
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

    public List<Month> getMonths() {
        return months;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
