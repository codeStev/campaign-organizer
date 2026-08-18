package com.campaignorganizer.worldbuilding.adapter.calendar.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Persistence model for a calendar month (maps the {@code calendar_months} table). */
@Entity
@Table(name = "calendar_months")
public class CalendarMonthJpaEntity {

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

    protected CalendarMonthJpaEntity() {
        // for JPA
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(UUID calendarId) {
        this.calendarId = calendarId;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
