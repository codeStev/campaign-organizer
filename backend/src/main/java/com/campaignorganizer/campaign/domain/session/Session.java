package com.campaignorganizer.campaign.domain.session;

import com.campaignorganizer.shared.domain.ConflictException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/** A play session within a campaign (aggregate root). */
public final class Session {

    /** The limit the session summary and notes already have at the web boundary. */
    public static final int MAX_TEXT_LENGTH = 20_000;

    private final UUID id;
    private final UUID campaignId;
    private String title;
    private Integer sessionNumber;
    private LocalDate date;
    private String summary;
    private String notes;
    private final Instant createdAt;
    private Instant updatedAt;

    private Session(UUID id, UUID campaignId, String title, Integer sessionNumber, LocalDate date,
                    String summary, String notes, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.campaignId = campaignId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(title, sessionNumber, date, summary, notes);
    }

    public static Session create(UUID id, UUID campaignId, String title, Integer sessionNumber,
                                 LocalDate date, String summary, String notes, Instant now) {
        return new Session(id, campaignId, title, sessionNumber, date, summary, notes, now, now);
    }

    public static Session reconstitute(UUID id, UUID campaignId, String title, Integer sessionNumber,
                                       LocalDate date, String summary, String notes, Instant createdAt,
                                       Instant updatedAt) {
        return new Session(id, campaignId, title, sessionNumber, date, summary, notes, createdAt,
                updatedAt);
    }

    public void update(String title, Integer sessionNumber, LocalDate date, String summary, String notes,
                       Instant now) {
        apply(title, sessionNumber, date, summary, notes);
        this.updatedAt = now;
    }

    /**
     * Writes the summary on behalf of an export (ADR-0121) without ever overwriting a summary the
     * caller did not write: accepted when there is no summary yet, or when {@code expectedUpdatedAt}
     * is the session's current update time (so nothing was edited since the caller last wrote it).
     */
    public void writeSummary(String summary, Instant expectedUpdatedAt, Instant now) {
        if (summary == null || summary.isBlank()) {
            throw new ValidationException("Summary must not be blank");
        }
        if (summary.length() > MAX_TEXT_LENGTH) {
            throw new ValidationException("Summary must be at most " + MAX_TEXT_LENGTH + " characters");
        }
        boolean hasSummary = this.summary != null && !this.summary.isBlank();
        boolean unchangedSince = expectedUpdatedAt != null
                && expectedUpdatedAt.truncatedTo(ChronoUnit.MICROS)
                        .equals(this.updatedAt.truncatedTo(ChronoUnit.MICROS));
        if (hasSummary && !unchangedSince) {
            throw new ConflictException("The session's summary was changed since the last export");
        }
        this.summary = summary;
        this.updatedAt = now;
    }

    public boolean hasSummary() {
        return summary != null && !summary.isBlank();
    }

    private void apply(String title, Integer sessionNumber, LocalDate date, String summary, String notes) {
        if (title == null || title.isBlank()) {
            throw new ValidationException("Session title must not be blank");
        }
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
