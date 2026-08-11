package com.campaignorganizer.calendar;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "calendar_months")
public class CalendarMonth {

    @Id
    private UUID id;

    @Column(name = "calendar_id", nullable = false, updatable = false)
    private UUID calendarId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int days;

    protected CalendarMonth() {
        // for JPA
    }

    public CalendarMonth(UUID calendarId, int position, String name, int days) {
        this.calendarId = calendarId;
        this.position = position;
        this.name = name;
        this.days = days;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getCalendarId() {
        return calendarId;
    }

    public int getPosition() {
        return position;
    }

    public String getName() {
        return name;
    }

    public int getDays() {
        return days;
    }
}
